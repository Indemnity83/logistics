package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsMod;
import com.logistics.LogisticsPipe;

import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.DispatchableModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.filter.FilterSlots;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.ProviderScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
public class ProviderModule implements Module, TickingModule, DispatchableModule {
    private static final String TICKS_SINCE_SCAN = "ticks_since_scan";
    public static final String MODE = "mode";
    public static final String FILTER_ITEMS = "filter_items";
    public static final String FILTER_INVERTED = "filter_inverted";
    // Dispatch queue NBT keys
    private static final String DISPATCH_QUEUE = "dispatch_queue";
    private static final String DQ_ITEM = "item";
    private static final String DQ_ITEM_TAG = "item_tag";
    private static final String DQ_AMOUNT = "amount";
    private static final String DQ_REQUESTER = "requester";
    private static final String DQ_DELIVERY_ID = "delivery_id";

    private static final int SCAN_INTERVAL = 6;        // Scan every 6 ticks (~3x/second)
    public static final int MAX_FILTER_SLOTS = 9;
    private static final int SUPPLY_PRIORITY = 1;      // Real stock; lower = preferred
    // Energy cost per item dispatched (drawn from the network battery).
    private static final long RF_PER_ITEM = 2;

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

    long calculateAvailableAmount(ProviderMode mode, long rawAmount, boolean isFirstSlotOfType) {
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

    public FilterSlots getFilterItems(PipeContext ctx) {
        return FilterSlots.load(ctx.getCompoundTag(this, FILTER_ITEMS), MAX_FILTER_SLOTS);
    }

    public void setFilterItem(PipeContext ctx, int slot, String itemId) {
        if (slot < 0 || slot >= MAX_FILTER_SLOTS) return;
        FilterSlots updated = getFilterItems(ctx).with(slot, itemId);
        if (updated.isEmpty()) {
            ctx.remove(this, FILTER_ITEMS);
        } else {
            ctx.putCompoundTag(this, FILTER_ITEMS, updated.toTag());
        }
        ctx.markDirtyAndSync();
    }

    public boolean isFilterInverted(PipeContext ctx) {
        return ctx.getInt(this, FILTER_INVERTED, 0) == 1;
    }

    @Override
    public void appendHud(PipeContext ctx, PipeHud hud) {
        // Filter first: an exclude filter shows "None", an empty filter "Any", otherwise the item icons.
        if (isFilterInverted(ctx)) {
            hud.line(ModuleHud.detail(Component.translatable("jade.logistics.pipe.filter.none")));
        } else {
            List<ItemStack> stacks = getFilterItems(ctx).asList().stream()
                    .map(id -> ModuleHud.stack(id, 1))
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            if (stacks.isEmpty()) {
                hud.line(ModuleHud.detail(Component.translatable("jade.logistics.pipe.filter.any")));
            } else {
                hud.items(stacks);
            }
        }
        hud.line(ModuleHud.detail(Component.translatable(
                "gui.logistics.provider.mode." + getMode(ctx).name().toLowerCase(Locale.ROOT))));
    }

    public void setFilterInverted(PipeContext ctx, boolean inverted) {
        ctx.saveInt(this, FILTER_INVERTED, inverted ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    private boolean isFilteredOut(PipeContext ctx, ItemStack stack) {
        FilterSlots filter = getFilterItems(ctx);
        return isFilterInverted(ctx) ? filter.included(stack) : filter.excluded(stack);
    }

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
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
    public long onDispatch(PipeContext ctx, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
        if (isFilteredOut(ctx, item.toStack(1))) return 0;
        if (ctx.getInventoryConnections().isEmpty()) return 0;

        enqueueDispatch(ctx, item, amount, requester, deliveryId);
        NetDbg.out("[Provider @ {}] Queued dispatch: {}x {} → {} (delivery {})",
                ctx.pos(), amount, item.toStack(1).getItem(), requester,
                deliveryId == null ? "none" : deliveryId.toString().substring(0, 8));
        return amount; // Promise: extraction happens from queue at rate-limited pace
    }

    private void enqueueDispatch(PipeContext ctx, IItemKey item, long amount,
            BlockPos requester, @Nullable UUID deliveryId) {
        ProviderDispatchQueue queue = loadQueue(ctx);
        String itemId = BuiltInRegistries.ITEM.getKey(item.toStack(1).getItem()).toString();
        RegistryOps<Tag> ops = ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag itemTag = ItemStack.CODEC.encodeStart(ops, item.toStack(1))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> (CompoundTag) t)
                .orElse(null);
        queue.enqueue(itemId, itemTag, amount, requester, deliveryId);
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
     *
     * <p>Draining an entry can advance the head to the next order, so the item is resolved from the
     * head on every iteration — entries in the same cycle may be for different items.
     */
    void processDispatchQueue(PipeContext ctx) {
        ProviderDispatchQueue queue = loadQueue(ctx);
        if (queue.isEmpty()) return;

        ProviderMode mode = getMode(ctx);
        long itemsLeft = itemLimit;
        int stacksLeft = stackLimit;

        while (itemsLeft > 0 && stacksLeft > 0) {
            ProviderDispatchQueue.Entry head = queue.peekHead();
            if (head == null) break;

            IItemKey item = resolveQueuedItem(ctx, head);
            if (item == null) { queue.removeHead(); break; }

            long toExtract = Math.min(head.remaining(), Math.min(itemsLeft, item.toStack(1).getMaxStackSize()));

            // Price the dispatch before committing to it: a simulated pass reports what a real
            // extraction would yield without touching the source, so an unpowered network stalls
            // with the items still in the machine instead of draining it a stack per cycle.
            Extraction planned = extractFromNeighbors(ctx, item, toExtract, mode, true);
            if (planned.amount() > 0 && !ctx.consumeEnergy(RF_PER_ITEM * planned.amount())) break;

            Extraction extraction = extractFromNeighbors(ctx, item, toExtract, mode, false);

            if (extraction.amount() > 0) {
                long extracted = extraction.amount();
                // A source whose simulate overstates what it gives up leaves the surplus charge
                // spent; one that understates it leaves the excess unpaid, so refund it rather than
                // dispatch items the network hasn't paid for.
                if (extracted > planned.amount()
                        && !ctx.consumeEnergy(RF_PER_ITEM * (extracted - planned.amount()))) {
                    refundExtraction(ctx, extraction, item, extracted);
                    break;
                }
                ItemStack stack = item.toStack((int) extracted);
                TravelingItem traveling = new TravelingItem(
                        stack, extraction.dir().getOpposite(), LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED), head.requester());
                traveling.setDeliveryId(head.deliveryId());
                ctx.pipeAccess().forceAddItem(traveling, extraction.dir());
                queue.consumeFromHead(extracted);
                itemsLeft -= extracted;
                stacksLeft -= 1;
                NetDbg.out("[Provider @ {}] Queue drain: {}x {} → {} ({} remaining)",
                        ctx.pos(), extracted, item.toStack(1).getItem(), head.requester(),
                        head.remaining() - extracted);
            } else {
                // Inventory empty or inaccessible — drop the entry so the queue doesn't block
                // forever, but also notify the network so in-flight accounting is released and
                // the request can be retried instead of being counted as delivered.
                ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
                if (network != null) {
                    if (head.deliveryId() != null) {
                        network.notifyDeliveryFailed(head.deliveryId(), head.requester(), item, head.remaining());
                    } else {
                        network.notifyDeliveryFailedNoId(head.requester(), item, head.remaining());
                    }
                }
                NetDbg.out("[Provider @ {}] Queue drain failed (empty?): {}x {} — dropping entry",
                        ctx.pos(), head.remaining(), item.toStack(1).getItem());
                queue.removeHead();
                break;
            }
        }

        saveQueue(ctx, queue);
    }

    /**
     * Rebuild the item key a queue entry was enqueued for, preferring the serialized stack so data
     * components survive. Returns {@code null} when the entry names an item this game no longer has.
     */
    @Nullable
    private IItemKey resolveQueuedItem(PipeContext ctx, ProviderDispatchQueue.Entry entry) {
        ResourceId rid = ResourceId.tryParse(entry.itemId());
        if (rid == null) return null;
        var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
        if (holder.isEmpty()) return null;

        if (entry.itemTag() != null) {
            RegistryOps<Tag> ops = ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack stack = ItemStack.CODEC.parse(ops, entry.itemTag()).result()
                    .orElse(new ItemStack(holder.get().value()));
            return ItemStorageLookup.of(stack);
        }
        return ItemStorageLookup.of(new ItemStack(holder.get().value()));
    }

    /** Items pulled from a neighboring inventory: the amount and where it came from (for refunds/routing). */
    private record Extraction(long amount, Direction dir, IItemStorage storage) {
        static final Extraction NONE = new Extraction(0, null, null);
    }

    /**
     * Scan each connected inventory in turn, extracting from the first that yields items.
     *
     * <p>With {@code simulate} the source is left untouched and the result only reports what a real
     * call would yield. A committed extraction is eager — the items are gone from the source as soon
     * as it returns — so the caller must refund via {@link Extraction#storage()} if it then can't
     * complete the dispatch.
     */
    private Extraction extractFromNeighbors(
            PipeContext ctx, IItemKey item, long toExtract, ProviderMode mode, boolean simulate) {
        for (Direction direction : ctx.getInventoryConnections()) {
            BlockPos targetPos = ctx.pos().relative(direction);
            IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());
            if (storage == null) continue;

            long got = extractItems(storage, item, toExtract, mode, simulate);
            if (got > 0) {
                return new Extraction(got, direction, storage);
            }
        }
        return Extraction.NONE;
    }

    /**
     * Put an unpayable extraction back where it came from.
     *
     * <p>{@code insert} is a partial-transfer API and the source may refuse some or all of it — an
     * output-only face such as a furnace's bottom, or an inventory that filled up in the same tick.
     * Whatever the source won't take is offered to the pipe's other inventories and, failing that,
     * dropped at the pipe, because it has already left the source and would otherwise be destroyed.
     */
    private void refundExtraction(PipeContext ctx, Extraction extraction, IItemKey item, long amount) {
        long remaining = amount - extraction.storage().insert(item, amount, false);
        if (remaining <= 0) return;

        for (Direction direction : ctx.getInventoryConnections()) {
            if (remaining <= 0) break;
            if (direction == extraction.dir()) continue;
            BlockPos targetPos = ctx.pos().relative(direction);
            IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());
            if (storage == null) continue;
            remaining -= storage.insert(item, remaining, false);
        }
        if (remaining <= 0) return;

        LogisticsMod.LOGGER.warn("[Provider @ {}] Nothing would take {}x {} back — dropping it at the pipe",
                ctx.pos(), remaining, item.toStack(1).getItem());
        dropAtPipe(ctx, item, remaining);
    }

    /** Spawn a refund nothing would re-accept as a ground item, so it stays recoverable. */
    private static void dropAtPipe(PipeContext ctx, IItemKey item, long amount) {
        Level world = ctx.world();
        if (world == null || world.isClientSide()) return;

        BlockPos pos = ctx.pos();
        long left = amount;
        while (left > 0) {
            int count = (int) Math.min(left, item.toStack(1).getMaxStackSize());
            ItemEntity entity = new ItemEntity(
                    world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.toStack(count));
            entity.setDefaultPickUpDelay();
            world.addFreshEntity(entity);
            left -= count;
        }
    }

    // ===== Queue NBT serialisation =====

    /** Load the dispatch queue from NBT state. Returns an empty queue if nothing is stored. */
    private ProviderDispatchQueue loadQueue(PipeContext ctx) {
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        ListTag tag = NbtCompat.getListOrEmpty(ctx.moduleState(this), DISPATCH_QUEUE);
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i).orElse(null);
            if (entry == null) continue;
            String itemId = NbtCompat.getString(entry, DQ_ITEM, "");
            long amount = NbtCompat.getLong(entry, DQ_AMOUNT, 0L);
            Optional<int[]> reqArr = entry.getIntArray(DQ_REQUESTER);
            String dlvStr = NbtCompat.getString(entry, DQ_DELIVERY_ID, "");
            if (itemId.isEmpty() || amount <= 0 || reqArr.isEmpty() || reqArr.get().length < 3) continue;
            int[] arr = reqArr.get();
            BlockPos requester = new BlockPos(arr[0], arr[1], arr[2]);
            UUID deliveryId = null;
            if (!dlvStr.isEmpty()) {
                try { deliveryId = UUID.fromString(dlvStr); } catch (Exception ignored) {}
            }
            CompoundTag itemTag = entry.getCompound(DQ_ITEM_TAG).orElse(null);
            queue.enqueue(itemId, itemTag, amount, requester, deliveryId);
        }
        return queue;
    }

    /** Persist the dispatch queue to NBT state. */
    void saveQueue(PipeContext ctx, ProviderDispatchQueue queue) {
        if (queue.isEmpty()) {
            ctx.moduleState(this).remove(DISPATCH_QUEUE);
        } else {
            ListTag tag = new ListTag();
            for (ProviderDispatchQueue.Entry e : queue.entries()) {
                CompoundTag entry = new CompoundTag();
                entry.putString(DQ_ITEM, e.itemId());
                if (e.itemTag() != null) entry.put(DQ_ITEM_TAG, e.itemTag());
                entry.putLong(DQ_AMOUNT, e.remaining());
                entry.putIntArray(DQ_REQUESTER, new int[]{e.requester().getX(), e.requester().getY(), e.requester().getZ()});
                if (e.deliveryId() != null) entry.putString(DQ_DELIVERY_ID, e.deliveryId().toString());
                tag.add(entry);
            }
            ctx.moduleState(this).put(DISPATCH_QUEUE, tag);
        }
        ctx.markDirtyAndSync();
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

        String moduleStateKey = ctx.moduleStateKey(this);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> new ProviderScreenHandler(
                        syncId, playerInventory, ((PipeBlockEntity) ctx.blockEntity()), moduleStateKey),
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
        network.registerSupply(ctx.pos(), toKeyMap(availableItems), SUPPLY_PRIORITY);

        if (!availableItems.isEmpty() && NetDbg.isEnabled()) {
            long totalItems = availableItems.values().stream().mapToLong(Long::longValue).sum();
            NetDbg.out("[Provider @ {}] Mode {} - Updated supply: {} item types, {} total",
                    ctx.pos(), mode, availableItems.size(), totalItems);
        }
    }

    private Map<ItemStack, Long> aggregateInventoryItems(PipeContext ctx, List<Direction> inventoryFaces, ProviderMode mode) {
        Map<IItemKey, ItemStack> keyToStack = new HashMap<>();
        Map<IItemKey, Long> keyAmounts = new HashMap<>();

        for (Direction direction : inventoryFaces) {
            scanInventoryAtDirection(ctx, direction, mode, keyToStack, keyAmounts);
        }

        // Subtract items already committed to queued dispatch entries so the supply table
        // does not double-count items that are reserved but not yet physically extracted.
        Map<String, Long> reservations = loadQueue(ctx).getReservations();
        if (!reservations.isEmpty()) {
            for (Map.Entry<IItemKey, Long> entry : new ArrayList<>(keyAmounts.entrySet())) {
                String id = BuiltInRegistries.ITEM.getKey(entry.getKey().toStack(1).getItem()).toString();
                long adjusted = ProviderDispatchQueue.subtractReservation(entry.getValue(), reservations, id);
                if (adjusted == 0) {
                    keyAmounts.remove(entry.getKey());
                    keyToStack.remove(entry.getKey());
                } else {
                    keyAmounts.put(entry.getKey(), adjusted);
                }
            }
        }

        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<IItemKey, Long> entry : keyAmounts.entrySet()) {
            result.put(keyToStack.get(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private void scanInventoryAtDirection(
            PipeContext ctx, Direction direction, ProviderMode mode,
            Map<IItemKey, ItemStack> keyToStack, Map<IItemKey, Long> keyAmounts) {

        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) return;

        List<IItemView> views = new ArrayList<>();
        for (IItemView view : storage.contents()) {
            views.add(view);
        }

        int startIndex = mode.getCropStart();
        int endIndex = Math.max(0, views.size() - mode.getCropEnd());
        Map<IItemKey, Boolean> firstSlotSeen = new HashMap<>();

        for (int i = startIndex; i < endIndex; i++) {
            IItemView view = views.get(i);
            IItemKey key = view.resource();
            long rawAmount = view.amount();
            if (rawAmount <= 0) continue;

            ItemStack stack = key.toStack(1);
            if (isFilteredOut(ctx, stack)) continue;

            boolean isFirstSlot = !firstSlotSeen.containsKey(key);
            firstSlotSeen.put(key, true);

            long adjustedAmount = calculateAvailableAmount(mode, rawAmount, isFirstSlot);
            if (adjustedAmount <= 0) continue;

            keyToStack.putIfAbsent(key, stack);
            keyAmounts.merge(key, adjustedAmount, Long::sum);
        }
    }

    // ==================== Item Extraction ====================

    private long extractItems(IItemStorage storage, IItemKey key, long requested, ProviderMode mode, boolean simulate) {
        List<IItemView> views = new ArrayList<>();
        for (IItemView view : storage.contents()) {
            if (view.amount() > 0) views.add(view);
        }

        int startIndex = mode.getCropStart();
        int endIndex = Math.max(0, views.size() - mode.getCropEnd());

        long totalExtracted = 0;
        long remaining = requested;
        boolean isFirstSlotOfType = true;

        for (int i = startIndex; i < endIndex && remaining > 0; i++) {
            IItemView view = views.get(i);
            if (!view.resource().equals(key)) continue;

            long available = view.amount();
            long adjustedAmount = calculateAvailableAmount(mode, available, isFirstSlotOfType);
            isFirstSlotOfType = false;
            if (adjustedAmount <= 0) continue;

            long canExtract = Math.min(adjustedAmount, remaining);
            long extracted = storage.extract(key, canExtract, simulate);
            totalExtracted += extracted;
            remaining -= extracted;
        }

        return totalExtracted;
    }

    // ==================== Helpers ====================

    /** Convert ItemStack→Long map to IItemKey→Long map for network supply. */
    private static Map<IItemKey, Long> toKeyMap(Map<ItemStack, Long> items) {
        Map<IItemKey, Long> result = new HashMap<>(items.size());
        for (Map.Entry<ItemStack, Long> entry : items.entrySet()) {
            result.merge(ItemStorageLookup.of(entry.getKey()), entry.getValue(), Long::sum);
        }
        return result;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        return false;
    }
}
