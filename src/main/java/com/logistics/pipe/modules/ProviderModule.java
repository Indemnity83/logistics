package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.network.LogisticsOrder;
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

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Provider module - scans ALL adjacent inventories and fulfills orders from the network.
 * Periodically updates the network cache with available items from all connected inventories.
 * When orders arrive, extracts items from any connected inventory and sends them with destination metadata.
 *
 * <p>Unlike ExtractionModule, Provider connects to ALL adjacent inventories simultaneously.
 * No wrench configuration needed - it automatically provides from any connected inventory.
 *
 * <p>Energy: TODO (Phase 11): 1 RF per item extracted
 */
public class ProviderModule implements Module {
    private static final String TICKS_SINCE_SCAN = "ticks_since_scan";
    private static final String MODE = "mode";
    private static final String FILTER_ITEMS = "filter_items";
    private static final String FILTER_INVERTED = "filter_inverted";
    private static final int SCAN_INTERVAL = 20; // Scan every 20 ticks (1 second)
    private static final int MAX_FILTER_SLOTS = 9;
    // TODO(Phase 11): Energy costs
    // private static final int RF_PER_ITEM = 1;

    /**
     * Provider modes control which items are available for extraction.
     * Each mode defines behavior through properties rather than scattered if-checks.
     */
    public enum ProviderMode {
        SUPPLY("Normal", false, false, 0),
        RESERVE("Leave 1 Stack", false, false, 64),
        SEEDED("Leave 1 Per Slot", true, false, 0),
        SAMPLE("Leave 1 Per Type", false, true, 0);

        private final String translationKey;
        private final boolean hideOnePerSlot;
        private final boolean hideOnePerType;
        private final int reserveAmount;

        ProviderMode(String translationKey, boolean hideOnePerSlot, boolean hideOnePerType, int reserveAmount) {
            this.translationKey = translationKey;
            this.hideOnePerSlot = hideOnePerSlot;
            this.hideOnePerType = hideOnePerType;
            this.reserveAmount = reserveAmount;
        }

        public boolean isHideOnePerSlot() { return hideOnePerSlot; }
        public boolean isHideOnePerType() { return hideOnePerType; }
        public int getReserveAmount() { return reserveAmount; }
        public String getTranslationKey() { return translationKey; }
    }

    // ==================== Mode Configuration ====================

    /**
     * Calculate available amount for provision based on provider mode.
     * Applies mode-specific hiding logic consistently.
     *
     * @param mode Provider mode
     * @param rawAmount Raw amount from inventory
     * @param isFirstSlotOfType True if this is the first slot containing this item type
     * @return Amount available for provision after mode logic
     */
    private long calculateAvailableAmount(ProviderMode mode, long rawAmount, boolean isFirstSlotOfType) {
        // Per-slot hiding (SEEDED mode)
        if (mode.isHideOnePerSlot()) {
            rawAmount = Math.max(0, rawAmount - 1);
        }

        // Per-type hiding (SAMPLE mode)
        if (mode.isHideOnePerType() && isFirstSlotOfType) {
            rawAmount = Math.max(0, rawAmount - 1);
        }

        // Note: Global reserve (RESERVE mode) is handled at aggregation level,
        // not per-slot, so it's applied in scanAndUpdateCache() after totaling.

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
            if (!itemId.isEmpty()) {
                items.add(itemId);
            }
        }
        return items;
    }

    public void setFilterItem(PipeContext ctx, int slot, String itemId) {
        if (slot < 0 || slot >= MAX_FILTER_SLOTS) {
            return;
        }
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

    /**
     * Check if an item should be filtered out (not provided).
     * @param ctx Pipe context
     * @param stack Item to check
     * @return true if the item should be filtered out, false if it should be provided
     */
    private boolean isFilteredOut(PipeContext ctx, ItemStack stack) {
        List<String> filterItems = getFilterItems(ctx);

        // Empty filter = provide all items (don't filter anything)
        if (filterItems.isEmpty()) {
            return false;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean itemInFilter = filterItems.contains(itemId);
        boolean isInverted = isFilterInverted(ctx);

        // Include mode: filter out items NOT in filter
        // Exclude mode: filter out items IN filter
        return isInverted == itemInFilter;
    }

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;

        int ticks = ctx.getInt(this, TICKS_SINCE_SCAN, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_SCAN, ticks);

        if (ticks >= SCAN_INTERVAL) {
            scanAndUpdateCache(ctx);
            ctx.saveInt(this, TICKS_SINCE_SCAN, 0);
        }

        processPendingOrders(ctx);
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        // Show feature arm on ALL inventory connections
        if (!ctx.isInventoryConnection(direction)) {
            return null;
        }
        return LogisticsPipe.model("provider_logistics_pipe_feature_extended");
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, net.minecraft.world.entity.player.Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> new ProviderScreenHandler(
                        syncId,
                        playerInventory,
                        ctx.blockEntity()
                ),
                Component.translatable("screen.logistics.provider")
        ));

        return InteractionResult.SUCCESS;
    }

    // ==================== Inventory Scanning ====================

    /**
     * Scan ALL adjacent inventories and update the network provider cache.
     */
    private void scanAndUpdateCache(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            network.updateProviderCache(ctx.pos(), new HashMap<>(), ctx.world().getGameTime());
            return;
        }

        ProviderMode mode = getMode(ctx);
        Map<ItemStack, Long> availableItems = aggregateInventoryItems(ctx, inventoryFaces, mode);
        availableItems = applyReserveMode(availableItems, mode);

        network.updateProviderCache(ctx.pos(), availableItems, ctx.world().getGameTime());
        logCacheUpdate(ctx, mode, availableItems);
    }

    /**
     * Scan all connected inventories and aggregate available items.
     */
    private Map<ItemStack, Long> aggregateInventoryItems(PipeContext ctx, List<Direction> inventoryFaces, ProviderMode mode) {
        Map<ItemVariant, ItemStack> variantToStack = new HashMap<>();
        Map<ItemVariant, Long> variantAmounts = new HashMap<>();

        for (Direction direction : inventoryFaces) {
            scanInventoryAtDirection(ctx, direction, mode, variantToStack, variantAmounts);
        }

        return convertToItemStackMap(variantToStack, variantAmounts);
    }

    /**
     * Scan a single inventory direction and accumulate items.
     */
    private void scanInventoryAtDirection(
            PipeContext ctx,
            Direction direction,
            ProviderMode mode,
            Map<ItemVariant, ItemStack> variantToStack,
            Map<ItemVariant, Long> variantAmounts) {

        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) return;

        Map<ItemVariant, Boolean> firstSlotSeen = new HashMap<>();

        for (StorageView<ItemVariant> view : storage) {
            ItemVariant variant = view.getResource();
            if (variant.isBlank()) continue;

            long rawAmount = view.getAmount();
            if (rawAmount <= 0) continue;

            ItemStack stack = variant.toStack();
            if (isFilteredOut(ctx, stack)) continue;

            boolean isFirstSlot = !firstSlotSeen.containsKey(variant);
            firstSlotSeen.put(variant, true);

            // Unified mode application
            long adjustedAmount = calculateAvailableAmount(mode, rawAmount, isFirstSlot);
            if (adjustedAmount <= 0) continue;

            variantToStack.putIfAbsent(variant, stack);
            variantAmounts.merge(variant, adjustedAmount, Long::sum);
        }
    }

    /**
     * Convert variant map to ItemStack map for network cache.
     */
    private Map<ItemStack, Long> convertToItemStackMap(
            Map<ItemVariant, ItemStack> variantToStack,
            Map<ItemVariant, Long> variantAmounts) {

        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemVariant, Long> entry : variantAmounts.entrySet()) {
            ItemStack stack = variantToStack.get(entry.getKey());
            result.put(stack, entry.getValue());
        }
        return result;
    }

    /**
     * Apply RESERVE mode to aggregated totals.
     * RESERVE mode leaves N items total across all slots (applied after aggregation).
     *
     * @param items Aggregated items from all slots
     * @param mode Provider mode
     * @return Items with RESERVE mode applied (if applicable)
     */
    private Map<ItemStack, Long> applyReserveMode(Map<ItemStack, Long> items, ProviderMode mode) {
        if (mode.getReserveAmount() == 0) return items;

        Map<ItemStack, Long> adjustedItems = new HashMap<>();
        for (Map.Entry<ItemStack, Long> entry : items.entrySet()) {
            long adjusted = Math.max(0, entry.getValue() - mode.getReserveAmount());
            if (adjusted > 0) {
                adjustedItems.put(entry.getKey(), adjusted);
            }
        }
        return adjustedItems;
    }

    /**
     * Log cache update results.
     */
    private void logCacheUpdate(PipeContext ctx, ProviderMode mode, Map<ItemStack, Long> availableItems) {
        if (!availableItems.isEmpty()) {
            long totalItems = availableItems.values().stream().mapToLong(Long::longValue).sum();
            LOGGER.info("[Provider @ {}] Mode {} - Updated cache: {} item types, {} total items",
                    ctx.pos(), mode, availableItems.size(), totalItems);
        } else if (mode != ProviderMode.SUPPLY) {
            LOGGER.info("[Provider @ {}] Mode {} - No items available after mode filter",
                    ctx.pos(), mode);
        }
    }

    // ==================== Order Processing ====================

    /**
     * Process pending orders from the network.
     * Tries to extract items from any connected inventory.
     */
    private void processPendingOrders(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        List<LogisticsOrder> orders = network.getOrdersFor(ctx.pos());
        if (orders.isEmpty()) return;

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) return;

        for (LogisticsOrder order : List.copyOf(orders)) {
            if (tryFulfillOrder(ctx, network, order, inventoryFaces)) {
                break;
            }
        }
    }

    /**
     * Try to fulfill an order from any connected inventory.
     * @return true if order was fulfilled (stop processing more orders this tick)
     */
    private boolean tryFulfillOrder(PipeContext ctx, ILogisticsNetwork network, LogisticsOrder order, List<Direction> inventoryFaces) {
        for (Direction direction : inventoryFaces) {
            BlockPos targetPos = ctx.pos().relative(direction);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
            if (storage == null) continue;

            if (fulfillOrder(ctx, storage, order, direction)) {
                network.removeOrder(order);
                return true;
            }
        }
        return false;
    }

    // ==================== Item Extraction ====================

    /**
     * Fulfill a single order by extracting items and creating a TravelingItem with destination.
     */
    private boolean fulfillOrder(PipeContext ctx, Storage<ItemVariant> storage, LogisticsOrder order, Direction direction) {
        // Double-check filter before fulfilling order
        if (isFilteredOut(ctx, order.stack())) {
            return false;
        }

        ItemVariant variant = ItemVariant.of(order.stack());
        ProviderMode mode = getMode(ctx);

        try (Transaction transaction = Transaction.openOuter()) {
            // Unified extraction for all modes
            long extracted = extractItems(storage, variant, order.amount(), transaction, mode);

            if (extracted > 0) {
                ItemStack stack = variant.toStack((int) extracted);
                // Create TravelingItem with destination at max speed for fast delivery
                TravelingItem item = new TravelingItem(
                    stack,
                    direction.getOpposite(),
                    LogisticsPipe.CONFIG.PROVIDER_PIPE_SPEED,
                    order.requester() // Set destination to requester position
                );
                ctx.blockEntity().forceAddItem(item, direction);
                transaction.commit();
                return true;
            }
        }

        return false;
    }

    /**
     * Extract items from storage respecting provider mode.
     * Maintains slot-position order by iterating through slots sequentially.
     *
     * @param storage Fabric ItemStorage
     * @param variant ItemVariant to extract
     * @param requested Amount requested
     * @param transaction Active transaction
     * @param mode Provider mode
     * @return Actual amount extracted
     */
    private long extractItems(Storage<ItemVariant> storage, ItemVariant variant, long requested, Transaction transaction, ProviderMode mode) {
        long totalExtracted = 0;
        long remaining = requested;
        boolean isFirstSlotOfType = true;

        // Iterate slots in order (maintains slot-position binding)
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            if (remaining <= 0) break;

            ItemVariant viewVariant = view.getResource();
            if (!viewVariant.equals(variant)) continue;

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

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // TODO(Phase 11): Accept energy for extraction costs
        return false; // For now, no energy required
    }
}
