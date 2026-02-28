package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.LogisticsOrder;
import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
     */
    public enum ProviderMode {
        SUPPLY,      // Provides all items (default)
        RESERVE,     // Leave first stack of each inventory
        GUARDED,     // Leave first and last stacks
        SEEDED,      // Leave 1 item per stack
        SAMPLE       // Leave 1 item per type
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

    public List<String> getFilterItems(PipeContext ctx) {
        CompoundTag filterItems = ctx.getCompoundTag(this, FILTER_ITEMS);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            String itemId = com.logistics.core.lib.storage.NbtCompat.getString(filterItems, String.valueOf(i), "");
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
     * Check if an item passes the filter.
     * @param ctx Pipe context
     * @param stack Item to check
     * @return true if the item should be provided, false otherwise
     */
    private boolean passesFilter(PipeContext ctx, ItemStack stack) {
        List<String> filterItems = getFilterItems(ctx);

        // Empty filter = provide all items
        if (filterItems.isEmpty()) {
            return true;
        }

        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean itemInFilter = filterItems.contains(itemId);
        boolean isInverted = isFilterInverted(ctx);

        // Include mode: only provide items in filter
        // Exclude mode: provide all items EXCEPT those in filter
        return isInverted ? !itemInFilter : itemInFilter;
    }

    /**
     * Apply the provider mode to determine how much of an item should be available.
     * @param ctx Pipe context
     * @param amount Raw amount from inventory
     * @return Amount available for provision after applying mode
     */
    private long applyMode(PipeContext ctx, long amount) {
        ProviderMode mode = getMode(ctx);

        return switch (mode) {
            case SUPPLY -> amount; // Provide everything
            case RESERVE -> Math.max(0, amount - 64); // Leave 1 stack (64 items)
            case GUARDED -> Math.max(0, amount - 128); // Leave 2 stacks (128 items)
            case SEEDED -> Math.max(0, amount - 1); // Leave 1 item per slot (simplified)
            case SAMPLE -> Math.max(0, amount - 1); // Leave 1 item per type
        };
    }

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) {
            return;
        }

        // Increment tick counter
        int ticks = ctx.getInt(this, TICKS_SINCE_SCAN, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_SCAN, ticks);

        // Scan inventory periodically
        if (ticks >= SCAN_INTERVAL) {
            scanAndUpdateCache(ctx);
            ctx.saveInt(this, TICKS_SINCE_SCAN, 0);
        }

        // Process pending orders
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
    public net.minecraft.world.InteractionResult onWrench(PipeContext ctx, net.minecraft.world.entity.player.Player player) {
        if (ctx.world().isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // Open Provider GUI
        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, playerInventory, p) -> new com.logistics.pipe.ui.ProviderScreenHandler(
                        syncId,
                        playerInventory,
                        ctx.blockEntity()
                ),
                net.minecraft.network.chat.Component.translatable("screen.logistics.provider")
        ));

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /**
     * Scan ALL adjacent inventories and update the network provider cache.
     */
    private void scanAndUpdateCache(PipeContext ctx) {
        // Ensure this pipe is part of a network (creates/joins on first tick after load)
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        // TODO(Phase 11): Check energy availability
        // if (ctx.getEnergy() < RF_PER_SCAN) return;

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            // No inventories - clear cache
            network.updateProviderCache(ctx.pos(), new HashMap<>(), ctx.world().getGameTime());
            return;
        }

        // Scan all connected inventories
        // Use ItemStack as key with proper aggregation
        Map<ItemStack, Long> availableItems = new HashMap<>();
        ProviderMode mode = getMode(ctx);

        // Track items by variant for proper merging
        Map<ItemVariant, ItemStack> variantToStack = new HashMap<>();
        Map<ItemVariant, Long> variantAmounts = new HashMap<>();

        for (Direction direction : inventoryFaces) {
            BlockPos targetPos = ctx.pos().relative(direction);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

            if (storage == null) {
                continue;
            }

            // Aggregate inventory contents by variant
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) {
                    continue;
                }

                long amount = view.getAmount();
                if (amount <= 0) {
                    continue;
                }

                ItemStack stack = variant.toStack();

                // Apply filter
                if (!passesFilter(ctx, stack)) {
                    continue;
                }

                // Apply SEEDED mode per-slot (leave 1 item in each slot)
                if (mode == ProviderMode.SEEDED) {
                    long originalAmount = amount;
                    amount = Math.max(0, amount - 1);
                    LOGGER.info("[Provider @ {}] SEEDED: {} slot - original: {}, providing: {}",
                            ctx.pos(), stack.getDisplayName().getString(), originalAmount, amount);
                    if (amount == 0) {
                        continue; // Skip if nothing left to provide
                    }
                }

                // Merge quantities by variant (this properly handles same items)
                variantToStack.putIfAbsent(variant, stack);
                variantAmounts.merge(variant, amount, Long::sum);
            }
        }

        // Convert variant map to ItemStack map
        for (Map.Entry<ItemVariant, Long> entry : variantAmounts.entrySet()) {
            ItemStack stack = variantToStack.get(entry.getKey());
            availableItems.put(stack, entry.getValue());
        }

        // Apply mode to aggregated amounts (RESERVE, GUARDED, SAMPLE)
        if (mode != ProviderMode.SUPPLY && mode != ProviderMode.SEEDED) {
            Map<ItemStack, Long> adjustedItems = new HashMap<>();
            for (Map.Entry<ItemStack, Long> entry : availableItems.entrySet()) {
                long original = entry.getValue();
                long adjusted = applyMode(ctx, original);
                LOGGER.info("[Provider @ {}] Mode {}: {} - original: {}, adjusted: {}",
                        ctx.pos(), mode, entry.getKey().getDisplayName().getString(), original, adjusted);
                if (adjusted > 0) {
                    adjustedItems.put(entry.getKey(), adjusted);
                }
            }
            availableItems = adjustedItems;
        }

        // Update network cache with aggregated contents
        network.updateProviderCache(ctx.pos(), availableItems, ctx.world().getGameTime());

        if (!availableItems.isEmpty()) {
            long totalItems = availableItems.values().stream().mapToLong(Long::longValue).sum();
            LOGGER.info("[Provider @ {}] Mode {} - Updated cache: {} item types, {} total items",
                    ctx.pos(), mode, availableItems.size(), totalItems);
        } else if (mode != ProviderMode.SUPPLY) {
            LOGGER.info("[Provider @ {}] Mode {} - No items available after mode filter",
                    ctx.pos(), mode);
        }
    }

    /**
     * Process pending orders from the network.
     * Try to extract items from ANY connected inventory.
     */
    private void processPendingOrders(PipeContext ctx) {
        // Ensure this pipe is part of a network (creates/joins on first tick after load)
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        List<LogisticsOrder> orders = network.getOrdersFor(ctx.pos());
        if (orders.isEmpty()) {
            return;
        }

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            return;
        }

        // Process orders one at a time
        for (LogisticsOrder order : List.copyOf(orders)) {
            // TODO(Phase 11): Check energy
            // long energyCost = order.amount() * RF_PER_ITEM;
            // if (ctx.getEnergy() < energyCost) continue;

            // Try to fulfill from any connected inventory
            boolean fulfilled = false;
            for (Direction direction : inventoryFaces) {
                BlockPos targetPos = ctx.pos().relative(direction);
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

                if (storage == null) {
                    continue;
                }

                fulfilled = fulfillOrder(ctx, storage, order, direction);
                if (fulfilled) {
                    network.removeOrder(order);
                    // TODO(Phase 11): Consume energy
                    // ctx.setEnergy(ctx.getEnergy() - energyCost);
                    break; // Order fulfilled, move to next order
                }
            }

            if (fulfilled) {
                break; // Process one order per tick
            }
        }
    }

    /**
     * Fulfill a single order by extracting items and creating a TravelingItem with destination.
     */
    private boolean fulfillOrder(PipeContext ctx, Storage<ItemVariant> storage, LogisticsOrder order, Direction direction) {
        // Double-check filter before fulfilling order
        if (!passesFilter(ctx, order.stack())) {
            return false;
        }

        ItemVariant variant = ItemVariant.of(order.stack());
        ProviderMode mode = getMode(ctx);

        try (Transaction transaction = Transaction.openOuter()) {
            long extracted;

            // SEEDED mode: manually extract from slots, never taking the last item from any slot
            if (mode == ProviderMode.SEEDED) {
                extracted = extractWithSeeding(storage, variant, order.amount(), transaction);
            } else {
                // Normal extraction for other modes
                extracted = storage.extract(variant, order.amount(), transaction);
            }

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
     * Extract items while respecting SEEDED mode: never extract the last item from any slot.
     */
    private long extractWithSeeding(Storage<ItemVariant> storage, ItemVariant variant, long requested, Transaction transaction) {
        long totalExtracted = 0;
        long remaining = requested;

        // Iterate through all storage views (slots)
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            if (remaining <= 0) {
                break;
            }

            ItemVariant viewVariant = view.getResource();
            if (!viewVariant.equals(variant)) {
                continue;
            }

            long available = view.getAmount();
            if (available <= 1) {
                // Keep the last item in this slot (seeded)
                continue;
            }

            // Extract at most (available - 1) to leave 1 item seeded
            long canExtract = Math.min(available - 1, remaining);
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
