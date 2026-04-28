package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.ItemAcceptingModule;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Passive Supplier module — acts as a sink for items already traveling through the network
 * whose type matches a configured supply slot and whose inventory stock is below the target.
 *
 * <p>Unlike {@link SupplierModule}, this module never places orders; it only intercepts
 * in-transit items passively. Configuration and GUI are identical to the Supplier module.
 */
public class PassiveSupplierModule extends SupplierModule implements ItemAcceptingModule {
    private final int priority;
    private static final String TICKS_SINCE_SYNC = "ticks_since_sync";

    public PassiveSupplierModule(int priority) {
        this.priority = priority;
    }
    private static final int SYNC_INTERVAL = 20;

    /** No active requesting — passive only, but periodically re-syncs sink interests. */
    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SINCE_SYNC, 0) + 1;
        if (ticks < SYNC_INTERVAL) {
            ctx.saveInt(this, TICKS_SINCE_SYNC, ticks);
            return;
        }
        ctx.saveInt(this, TICKS_SINCE_SYNC, 0);
        syncInterests(ctx);
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        super.onConnectionsChanged(ctx, options);
        if (!ctx.world().isClientSide()) syncInterests(ctx);
    }

    @Override
    public void onDetach(PipeContext ctx) {
        super.onDetach(ctx);
        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = ctx.network();
            if (network != null) network.unregisterSink(ctx.pos()); // also clears interests
        }
    }

    private void syncInterests(PipeContext ctx) {
        ILogisticsNetwork network = ctx.network();
        if (network == null) return;
        network.registerSink(ctx.pos(), priority);
        network.unregisterSinkInterests(ctx.pos());
        int interestCount = 0;
        for (SupplyConfig config : getSupplyConfigs(ctx)) {
            if (config.itemId().isEmpty() || config.amount() <= 0) continue;
            ResourceId rid = ResourceId.tryParse(config.itemId());
            if (rid == null) continue;
            Item syncItem = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (syncItem != null && syncItem != Items.AIR) {
                network.registerSinkInterest(ctx.pos(), syncItem);
                interestCount++;
            }
        }
        NetDbg.out("[PassiveSupplier @ {}] Synced interests: {} items", ctx.pos(), interestCount);
    }

    /**
     * Returns true if this module is configured for the given item AND the adjacent
     * inventory is below the configured target amount (i.e., the module would accept it).
     */
    public boolean matchesItem(PipeContext ctx, ItemStack stack) {
        Direction sinkDir = getSupplierDirection(ctx);
        if (sinkDir == null) return false;
        for (SupplyConfig config : getSupplyConfigs(ctx)) {
            if (config.itemId().isEmpty() || config.amount() <= 0) continue;
            ResourceId rid = ResourceId.tryParse(config.itemId());
            if (rid == null) continue;
            Item matchItem = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (matchItem == null || matchItem == Items.AIR) continue;
            if (stack.getItem() != matchItem) continue;
            if (scanInventory(ctx, sinkDir, stack) < config.amount()) return true;
        }
        return false;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        Direction sinkDir = getSupplierDirection(ctx);
        if (sinkDir != null && options.contains(sinkDir)) {
            ItemStack stack = item.getStack();
            for (SupplyConfig config : getSupplyConfigs(ctx)) {
                if (config.itemId().isEmpty() || config.amount() <= 0) continue;
                var rid = com.logistics.core.lib.resource.ResourceId.tryParse(config.itemId());
                if (rid == null) continue;
                Item routeItem = BuiltInRegistries.ITEM.get(rid.toIdentifier());
                if (routeItem == null || routeItem == Items.AIR) continue;
                if (stack.getItem() != routeItem) continue;

                long currentStock = scanInventory(ctx, sinkDir, stack);
                if (currentStock < config.amount()) {
                    NetDbg.out("[PassiveSupplier @ {}] Intercepting {} (current={}, target={})", ctx.pos(), stack.getItem(), currentStock, config.amount());
                    return RoutePlan.reroute(sinkDir);
                }
                NetDbg.out("[PassiveSupplier @ {}] Passing on {} (current={} >= target={})", ctx.pos(), stack.getItem(), currentStock, config.amount());
            }
        }

        // Items already addressed to this pipe still get delivered
        return super.route(ctx, item, options);
    }

    @Override
    public boolean acceptsItem(PipeContext ctx, ItemStack stack) {
        return matchesItem(ctx, stack);
    }

    private long scanInventory(PipeContext ctx, Direction direction, ItemStack target) {
        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage =
                ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) return 0;

        // Config stores item ID only (no component data), so count all variants of the
        // same item type. This prevents overfilling when enchanted/damaged items arrive
        // for a stock target configured by item type alone.
        long count = 0;
        for (StorageView<ItemVariant> view : storage) {
            if (view.getResource().getItem() == target.getItem()) {
                count += view.getAmount();
            }
        }
        return count;
    }
}
