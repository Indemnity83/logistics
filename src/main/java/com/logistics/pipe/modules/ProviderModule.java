package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Provider module - scans all adjacent inventories and fulfills orders from the network.
 * Periodically updates the network supply table with available items from all connected inventories.
 * When the network dispatches an order (via {@link #onDispatch}), extracts items from any connected
 * inventory, creates a TravelingItem destined for the requester, and updates the supply table.
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
    private static final String MODE = "mode";
    private static final String FILTER_ITEMS = "filter_items";
    private static final String FILTER_INVERTED = "filter_inverted";
    private static final int SCAN_INTERVAL = 6;        // Scan every 6 ticks (~3x/second)
    private static final int ITEMS_PER_EXTRACT = 8;    // Max items extracted per dispatch
    private static final int MAX_FILTER_SLOTS = 9;
    private static final int SUPPLY_PRIORITY = 1;      // Real stock; lower = preferred

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

    public ProviderModule() {}

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
            scanAndUpdateSupply(ctx);
            ctx.saveInt(this, TICKS_SINCE_SCAN, 0);
        }
    }

    /**
     * Called by the network to extract and dispatch items synchronously.
     * Extracts up to {@code amount} (capped at ITEMS_PER_EXTRACT) from any connected inventory,
     * injects a TravelingItem into the pipe, and refreshes the supply table.
     */
    @Override
    public long onDispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        if (isFilteredOut(ctx, item.toStack())) return 0;

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) return 0;

        ProviderMode mode = getMode(ctx);
        long toExtract = Math.min(amount, ITEMS_PER_EXTRACT);

        for (Direction direction : inventoryFaces) {
            BlockPos targetPos = ctx.pos().relative(direction);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
            if (storage == null) continue;

            try (Transaction transaction = Transaction.openOuter()) {
                long extracted = extractItems(storage, item, toExtract, transaction, mode);
                if (extracted > 0) {
                    ItemStack stack = item.toStack((int) extracted);
                    TravelingItem traveling = new TravelingItem(
                            stack, direction.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED, requester);
                    traveling.setDeliveryId(deliveryId);
                    ctx.blockEntity().forceAddItem(traveling, direction);
                    transaction.commit();
                    // Re-register updated supply so subsequent dispatch calls see accurate stock
                    scanAndUpdateSupply(ctx);
                    return extracted;
                }
            }
        }
        return 0;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!ctx.isInventoryConnection(direction)) return null;
        return LogisticsPipe.model("provider_logistics_pipe_feature_extended");
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

        if (!availableItems.isEmpty()) {
            long totalItems = availableItems.values().stream().mapToLong(Long::longValue).sum();
            LOGGER.debug("[Provider @ {}] Mode {} - Updated supply: {} item types, {} total",
                    ctx.pos(), mode, availableItems.size(), totalItems);
        }
    }

    private Map<ItemStack, Long> aggregateInventoryItems(PipeContext ctx, List<Direction> inventoryFaces, ProviderMode mode) {
        Map<ItemVariant, ItemStack> variantToStack = new HashMap<>();
        Map<ItemVariant, Long> variantAmounts = new HashMap<>();

        for (Direction direction : inventoryFaces) {
            scanInventoryAtDirection(ctx, direction, mode, variantToStack, variantAmounts);
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
