package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.ProviderScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provider module - scans all adjacent inventories and fulfills orders from the network.
 * Periodically updates the network supply table with available items from all connected inventories.
 *
 * <p>Dispatch is asynchronous: when the network calls {@link #onDispatch}, the request is added to
 * an internal dispatch queue and the full amount is returned as a promise. Every {@code SCAN_INTERVAL}
 * ticks the provider extracts up to {@code ITEMS_PER_EXTRACT} items from the queue head and injects
 * a TravelingItem into the pipe. This decouples network ordering from physical extraction, preventing
 * flooding when large orders are placed.
 *
 * <p>Supply scans subtract queue reservations so the network never double-counts items that are
 * already committed to a queued delivery.
 *
 * <p>Provider modes control which items are available:
 * <ul>
 *   <li>SUPPLY - Provide all items</li>
 *   <li>RESERVE - Skip first inventory slot</li>
 *   <li>GUARDED - Skip first and last inventory slots</li>
 *   <li>SEEDED - Leave 1 item in each slot</li>
 *   <li>SAMPLE - Leave 1 item of each type</li>
 * </ul>
 */
public class ProviderModule implements Module {
    private static final String TICKS_SINCE_SCAN = "ticks_since_scan";
    public static final String MODE = "mode";
    public static final String FILTER_ITEMS = "filter_items";
    public static final String FILTER_INVERTED = "filter_inverted";
    // Dispatch queue NBT keys (values kept short to minimise NBT size)
    private static final String DISPATCH_QUEUE = "dispatch_queue";
    private static final String DQ_ITEM = "i";  // item registry key string
    private static final String DQ_AMT  = "a";  // remaining amount (long)
    private static final String DQ_REQ  = "r";  // requester "x,y,z"
    private static final String DQ_DLV  = "d";  // delivery UUID string (optional)

    private static final int SCAN_INTERVAL = 6;        // Scan every 6 ticks (~3x/second)
    private static final int MAX_FILTER_SLOTS = 9;
    private static final int SUPPLY_PRIORITY = 1;      // Real stock; lower = preferred

    private final int itemLimit;
    private final int stackLimit;

    /**
     * Provider modes control which items are available for extraction.
     */
    public enum ProviderMode {
        SUPPLY("Normal", false, false, 0, 0),
        RESERVE("Leave First Slot", false, false, 1, 0),
        GUARDED("Leave First & Last Slot", false, false, 1, 1),
        SEEDED("Leave 1 Per Slot", true, false, 0, 0),
        SAMPLE("Leave 1 Per Type", false, true, 0, 0);

        private final String translationKey;
        private final boolean hideOnePerSlot;
        private final boolean hideOnePerType;
        private final int cropStart;
        private final int cropEnd;

        ProviderMode(String translationKey, boolean hideOnePerSlot, boolean hideOnePerType, int cropStart, int cropEnd) {
            this.translationKey = translationKey;
            this.hideOnePerSlot = hideOnePerSlot;
            this.hideOnePerType = hideOnePerType;
            this.cropStart = cropStart;
            this.cropEnd = cropEnd;
        }

        public boolean isHideOnePerSlot() { return hideOnePerSlot; }
        public boolean isHideOnePerType() { return hideOnePerType; }
        public int getCropStart() { return cropStart; }
        public int getCropEnd() { return cropEnd; }
        public String getTranslationKey() { return translationKey; }
    }

    public ProviderModule(int itemLimit, int stackLimit) {
        this.itemLimit = itemLimit;
        this.stackLimit = stackLimit;
    }

    // ==================== Mode Configuration ====================

    private long calculateAvailableAmount(ProviderMode mode, long rawAmount, boolean isFirstSlotOfType) {
        if (mode.isHideOnePerSlot()) rawAmount = Math.max(0, rawAmount - 1);
        if (mode.isHideOnePerType() && isFirstSlotOfType) rawAmount = Math.max(0, rawAmount - 1);
        return rawAmount;
    }

    public ProviderMode getMode(PipeContext ctx) {
        int ordinal = ctx.getInt(this, MODE, ProviderMode.SUPPLY.ordinal());
        return ProviderMode.values()[ordinal];
    }

    public int getModeOrdinal(PipeContext ctx) {
        return ctx.getInt(this, MODE, ProviderMode.SUPPLY.ordinal());
    }

    public void setMode(PipeContext ctx, ProviderMode mode) {
        ctx.saveInt(this, MODE, mode.ordinal());
        ctx.markDirtyAndSync();
    }

    public void setModeFromOrdinal(PipeContext ctx, int ordinal) {
        if (ordinal < 0 || ordinal >= ProviderMode.values().length) {
            throw new IllegalArgumentException("Invalid mode ordinal: " + ordinal);
        }
        setMode(ctx, ProviderMode.values()[ordinal]);
    }

    // ==================== Filter Configuration ====================

    public List<String> getFilterItems(PipeContext ctx) {
        CompoundTag filterItems = ctx.getCompoundTag(this, FILTER_ITEMS);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            String itemId = NbtCompat.getString(filterItems, String.valueOf(i), "");
            if (!itemId.isEmpty()) items.add(itemId);
        }
        return items;
    }

    public void setFilterItem(PipeContext ctx, int slot, String itemId) {
        if (slot < 0 || slot >= MAX_FILTER_SLOTS) return;
        CompoundTag filterItems = ctx.getCompoundTag(this, FILTER_ITEMS);
        if (itemId == null || itemId.isEmpty()) {
            filterItems.remove(String.valueOf(slot));
        } else {
            filterItems.putString(String.valueOf(slot), itemId);
        }
        ctx.putCompoundTag(this, FILTER_ITEMS, filterItems);
        ctx.markDirtyAndSync();
    }

    public boolean isFilterInverted(PipeContext ctx) {
        return ctx.getInt(this, FILTER_INVERTED, 0) == 1;
    }

    public void setFilterInverted(PipeContext ctx, boolean inverted) {
        ctx.saveInt(this, FILTER_INVERTED, inverted ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    private boolean isFilteredOut(PipeContext ctx, ItemStack stack) {
        List<String> filterItems = getFilterItems(ctx);
        if (filterItems.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean itemInFilter = filterItems.contains(itemId);
        return isFilterInverted(ctx) == itemInFilter;
    }

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;

        int ticks = ctx.getInt(this, TICKS_SINCE_SCAN, 0) + 1;
        ctx.saveInt(this, TICKS_SINCE_SCAN, ticks);

        if (ticks >= SCAN_INTERVAL) {
            scanAndUpdateSupply(ctx);    // Update supply (subtracts queue reservations)
            processDispatchQueue(ctx);   // Drain one batch from the queue
            ctx.saveInt(this, TICKS_SINCE_SCAN, 0);
        }
    }

    /**
     * Called by the network to accept a dispatch order.
     * Enqueues the request and returns {@code amount} as a promise; physical extraction
     * happens asynchronously in {@link #onTick} at {@code ITEMS_PER_EXTRACT} per
     * {@code SCAN_INTERVAL} ticks. This prevents flooding the pipe with many TravelingItems
     * when the network places a large order.
     */
    @Override
    public long onDispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        if (isFilteredOut(ctx, item.toStack())) return 0;
        if (ctx.getInventoryConnections().isEmpty()) return 0;

        enqueueDispatch(ctx, item, amount, requester, deliveryId);
        NetDbg.out("[Provider @ {}] Queued dispatch: {}x {} → {} (delivery {})",
                ctx.pos(), amount, item.toStack().getItem(), requester,
                deliveryId == null ? "none" : deliveryId.toString().substring(0, 8));
        return amount; // Promise: extraction happens from queue at rate-limited pace
    }

    private void enqueueDispatch(PipeContext ctx, ItemVariant item, long amount,
            BlockPos requester, @Nullable UUID deliveryId) {
        ProviderDispatchQueue queue = loadQueue(ctx);
        queue.enqueue(BuiltInRegistries.ITEM.getKey(item.getItem()).toString(), amount, requester, deliveryId);
        saveQueue(ctx, queue);
    }

    /**
     * Drain the dispatch queue head using dual simultaneous caps:
     * <ul>
     *   <li>{@code itemLimit} — total items budget across all stacks this cycle</li>
     *   <li>{@code stackLimit} — max number of TravelingItems (stacks) injected this cycle</li>
     * </ul>
     * Each iteration extracts one stack (up to {@code itemsLeft} items), injects a TravelingItem,
     * then decrements both {@code itemsLeft} and {@code stacksLeft}. The loop stops as soon as
     * either limit is exhausted or the queue is empty.
     */
    private void processDispatchQueue(PipeContext ctx) {
        ProviderDispatchQueue queue = loadQueue(ctx);
        if (queue.isEmpty()) return;

        ProviderDispatchQueue.Entry head = queue.peekHead();
        if (head == null) return;

        ResourceId rid = ResourceId.tryParse(head.itemId());
        if (rid == null) { queue.removeHead(); saveQueue(ctx, queue); return; }
        var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
        if (holder == null) { queue.removeHead(); saveQueue(ctx, queue); return; }
        ItemVariant item = ItemVariant.of(new ItemStack(holder.asItem()));

        ProviderMode mode = getMode(ctx);
        long itemsLeft = itemLimit;
        int stacksLeft = stackLimit;

        while (itemsLeft > 0 && stacksLeft > 0) {
            head = queue.peekHead();
            if (head == null) break;

            long toExtract = Math.min(head.remaining(), Math.min(itemsLeft, item.toStack(1).getMaxStackSize()));
            long extracted = 0;
            Direction extractDir = null;

            for (Direction direction : ctx.getInventoryConnections()) {
                BlockPos targetPos = ctx.pos().relative(direction);
                Storage<ItemVariant> storage =
                        ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
                if (storage == null) continue;

                try (Transaction transaction = Transaction.openOuter()) {
                    long got = extractItems(storage, item, toExtract, transaction, mode);
                    if (got > 0) {
                        transaction.commit();
                        extracted = got;
                        extractDir = direction;
                        break;
                    }
                }
            }

            if (extracted > 0) {
                ItemStack stack = item.toStack((int) extracted);
                TravelingItem traveling = new TravelingItem(
                        stack, extractDir.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED, head.requester());
                traveling.setDeliveryId(head.deliveryId());
                ctx.blockEntity().forceAddItem(traveling, extractDir);
                queue.consumeFromHead(extracted);
                itemsLeft -= extracted;
                stacksLeft -= 1;
                NetDbg.out("[Provider @ {}] Queue drain: {}x {} → {} ({} remaining)",
                        ctx.pos(), extracted, item.toStack().getItem(), head.requester(),
                        head.remaining() - extracted);
            } else {
                // Inventory empty or inaccessible — drop the entry so the queue doesn't block
                // forever, but also notify the network so orderedForRequester is decremented.
                // Without the notify call, orderedForRequester would stay elevated permanently
                // (it only decrements on physical TravelingItem delivery), which would prevent
                // suppliers from re-ordering these items.
                ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
                if (network != null) {
                    network.notifyDelivery(head.requester(), item, head.remaining());
                }
                NetDbg.out("[Provider @ {}] Queue drain failed (empty?): {}x {} — dropping entry",
                        ctx.pos(), head.remaining(), item.toStack().getItem());
                queue.removeHead();
                break;
            }
        }

        saveQueue(ctx, queue);
    }

    // ===== Queue NBT serialisation =====

    /** Load the dispatch queue from NBT state. Returns an empty queue if nothing is stored. */
    private ProviderDispatchQueue loadQueue(PipeContext ctx) {
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        ListTag tag = NbtCompat.getListOrEmpty(ctx.moduleState(getStateKey()), DISPATCH_QUEUE);
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            if (entry == null) continue;
            String itemId    = NbtCompat.getString(entry, DQ_ITEM, "");
            long   amount    = NbtCompat.getLong(entry, DQ_AMT, 0L);
            String reqStr    = NbtCompat.getString(entry, DQ_REQ, "");
            String dlvStr    = NbtCompat.getString(entry, DQ_DLV, "");
            if (itemId.isEmpty() || amount <= 0 || reqStr.isEmpty()) continue;
            BlockPos requester = parseBlockPos(reqStr);
            if (requester == null) continue;
            UUID deliveryId = null;
            if (!dlvStr.isEmpty()) {
                try { deliveryId = UUID.fromString(dlvStr); } catch (Exception ignored) {}
            }
            queue.enqueue(itemId, amount, requester, deliveryId);
        }
        return queue;
    }

    /** Persist the dispatch queue to NBT state. */
    private void saveQueue(PipeContext ctx, ProviderDispatchQueue queue) {
        if (queue.isEmpty()) {
            ctx.moduleState(getStateKey()).remove(DISPATCH_QUEUE);
        } else {
            ListTag tag = new ListTag();
            for (ProviderDispatchQueue.Entry e : queue.entries()) {
                CompoundTag entry = new CompoundTag();
                entry.putString(DQ_ITEM, e.itemId());
                entry.putLong(DQ_AMT, e.remaining());
                entry.putString(DQ_REQ, e.requester().getX() + "," + e.requester().getY() + "," + e.requester().getZ());
                if (e.deliveryId() != null) entry.putString(DQ_DLV, e.deliveryId().toString());
                tag.add(entry);
            }
            ctx.moduleState(getStateKey()).put(DISPATCH_QUEUE, tag);
        }
        ctx.markDirtyAndSync();
    }

    @Nullable
    private static BlockPos parseBlockPos(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(",");
            return new BlockPos(
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!ctx.isInventoryConnection(direction)) return null;
        return LogisticsPipe.model("provider_logistics_pipe_feature_extended");
    }

    @Override
    public void onDetach(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network != null) {
            network.registerSupply(ctx.pos(), new HashMap<>(), SUPPLY_PRIORITY);
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, net.minecraft.world.entity.player.Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> new ProviderScreenHandler(
                        syncId, playerInventory, ctx.blockEntity()),
                Component.translatable("screen.logistics.provider")));

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ProviderScreenHandler(syncId, inv, player, hand),
                Component.translatable("screen.logistics.provider")));
        return InteractionResult.SUCCESS;
    }

    // ==================== Inventory Scanning ====================

    /**
     * Scan all adjacent inventories and register available supply with the network.
     */
    private void scanAndUpdateSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            network.registerSupply(ctx.pos(), new HashMap<>(), SUPPLY_PRIORITY);
            return;
        }

        ProviderMode mode = getMode(ctx);
        Map<ItemStack, Long> availableItems = aggregateInventoryItems(ctx, inventoryFaces, mode);
        network.registerSupply(ctx.pos(), toVariantMap(availableItems), SUPPLY_PRIORITY);

        if (!availableItems.isEmpty() && NetDbg.isEnabled()) {
            long totalItems = availableItems.values().stream().mapToLong(Long::longValue).sum();
            NetDbg.out("[Provider @ {}] Mode {} - Updated supply: {} item types, {} total",
                    ctx.pos(), mode, availableItems.size(), totalItems);
        }
    }

    private Map<ItemStack, Long> aggregateInventoryItems(PipeContext ctx, List<Direction> inventoryFaces, ProviderMode mode) {
        Map<ItemVariant, ItemStack> variantToStack = new HashMap<>();
        Map<ItemVariant, Long> variantAmounts = new HashMap<>();

        for (Direction direction : inventoryFaces) {
            scanInventoryAtDirection(ctx, direction, mode, variantToStack, variantAmounts);
        }

        // Subtract items already committed to queued dispatch entries so the supply table
        // does not double-count items that are reserved but not yet physically extracted.
        Map<String, Long> reservations = loadQueue(ctx).getReservations();
        if (!reservations.isEmpty()) {
            for (Map.Entry<ItemVariant, Long> entry : new ArrayList<>(variantAmounts.entrySet())) {
                String id = BuiltInRegistries.ITEM.getKey(entry.getKey().getItem()).toString();
                long adjusted = ProviderDispatchQueue.subtractReservation(entry.getValue(), reservations, id);
                if (adjusted == 0) {
                    variantAmounts.remove(entry.getKey());
                    variantToStack.remove(entry.getKey());
                } else {
                    variantAmounts.put(entry.getKey(), adjusted);
                }
            }
        }

        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemVariant, Long> entry : variantAmounts.entrySet()) {
            result.put(variantToStack.get(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private void scanInventoryAtDirection(
            PipeContext ctx, Direction direction, ProviderMode mode,
            Map<ItemVariant, ItemStack> variantToStack, Map<ItemVariant, Long> variantAmounts) {

        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) return;

        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (StorageView<ItemVariant> view : storage) {
            views.add(view);
        }

        int startIndex = mode.getCropStart();
        int endIndex = Math.max(0, views.size() - mode.getCropEnd());
        Map<ItemVariant, Boolean> firstSlotSeen = new HashMap<>();

        for (int i = startIndex; i < endIndex; i++) {
            StorageView<ItemVariant> view = views.get(i);
            ItemVariant variant = view.getResource();
            if (variant.isBlank()) continue;

            long rawAmount = view.getAmount();
            if (rawAmount <= 0) continue;

            ItemStack stack = variant.toStack();
            if (isFilteredOut(ctx, stack)) continue;

            boolean isFirstSlot = !firstSlotSeen.containsKey(variant);
            firstSlotSeen.put(variant, true);

            long adjustedAmount = calculateAvailableAmount(mode, rawAmount, isFirstSlot);
            if (adjustedAmount <= 0) continue;

            variantToStack.putIfAbsent(variant, stack);
            variantAmounts.merge(variant, adjustedAmount, Long::sum);
        }
    }

    // ==================== Item Extraction ====================

    private long extractItems(Storage<ItemVariant> storage, ItemVariant variant, long requested,
            Transaction transaction, ProviderMode mode) {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            views.add(view);
        }

        int startIndex = mode.getCropStart();
        int endIndex = Math.max(0, views.size() - mode.getCropEnd());

        long totalExtracted = 0;
        long remaining = requested;
        boolean isFirstSlotOfType = true;

        for (int i = startIndex; i < endIndex && remaining > 0; i++) {
            StorageView<ItemVariant> view = views.get(i);
            if (!view.getResource().equals(variant)) continue;

            long available = view.getAmount();
            long adjustedAmount = calculateAvailableAmount(mode, available, isFirstSlotOfType);
            isFirstSlotOfType = false;
            if (adjustedAmount <= 0) continue;

            long canExtract = Math.min(adjustedAmount, remaining);
            long extracted = view.extract(variant, canExtract, transaction);
            totalExtracted += extracted;
            remaining -= extracted;
        }

        return totalExtracted;
    }

    // ==================== Helpers ====================

    /** Convert ItemStack→Long map to ItemVariant→Long map for network supply. */
    private static Map<ItemVariant, Long> toVariantMap(Map<ItemStack, Long> items) {
        Map<ItemVariant, Long> result = new HashMap<>(items.size());
        for (Map.Entry<ItemStack, Long> entry : items.entrySet()) {
            result.merge(ItemVariant.of(entry.getKey()), entry.getValue(), Long::sum);
        }
        return result;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        return false;
    }
}
