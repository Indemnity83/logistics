package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.LogisticsOrder;
import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.PipeNetwork;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int SCAN_INTERVAL = 20; // Scan every 20 ticks (1 second)
    // TODO(Phase 11): Energy costs
    // private static final int RF_PER_ITEM = 1;

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
        return LogisticsPipe.model("provider_pipe_feature_extended");
    }

    /**
     * Scan ALL adjacent inventories and update the network provider cache.
     */
    private void scanAndUpdateCache(PipeContext ctx) {
        PipeNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
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
        Map<ItemStack, Long> availableItems = new HashMap<>();
        for (Direction direction : inventoryFaces) {
            BlockPos targetPos = ctx.pos().relative(direction);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

            if (storage == null) {
                continue;
            }

            // Aggregate inventory contents
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
                // Merge quantities if same item exists in multiple inventories
                availableItems.merge(stack, amount, Long::sum);
            }
        }

        // Update network cache with aggregated contents
        network.updateProviderCache(ctx.pos(), availableItems, ctx.world().getGameTime());
    }

    /**
     * Process pending orders from the network.
     * Try to extract items from ANY connected inventory.
     */
    private void processPendingOrders(PipeContext ctx) {
        PipeNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
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
        ItemVariant variant = ItemVariant.of(order.stack());

        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(variant, order.amount(), transaction);
            if (extracted > 0) {
                ItemStack stack = variant.toStack((int) extracted);
                // Create TravelingItem with destination
                TravelingItem item = new TravelingItem(
                    stack,
                    direction.getOpposite(),
                    LogisticsPipe.CONFIG.ITEM_MIN_SPEED,
                    order.requester() // Set destination to requester position
                );
                ctx.blockEntity().forceAddItem(item, direction);
                transaction.commit();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // TODO(Phase 11): Accept energy for extraction costs
        return false; // For now, no energy required
    }
}
