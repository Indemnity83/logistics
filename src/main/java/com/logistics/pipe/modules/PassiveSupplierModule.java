package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Passive Supplier module — acts as a sink for items already traveling through the network
 * whose type matches a configured supply slot and whose inventory stock is below the target.
 *
 * <p>Unlike {@link SupplierModule}, this module never places orders; it only intercepts
 * in-transit items passively. Configuration and GUI are identical to the Supplier module.
 */
public class PassiveSupplierModule extends SupplierModule {

    /** No active requesting — passive only. */
    @Override
    public void onTick(PipeContext ctx) {}

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        Direction sinkDir = getSupplierDirection(ctx);
        if (sinkDir != null && options.contains(sinkDir)) {
            ItemStack stack = item.getStack();
            for (SupplyConfig config : getSupplyConfigs(ctx)) {
                if (config.itemId().isEmpty() || config.amount() <= 0) continue;
                var rid = com.logistics.core.lib.resource.ResourceId.tryParse(config.itemId());
                if (rid == null) continue;
                var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
                if (holder.isEmpty()) continue;
                if (stack.getItem() != holder.get().value()) continue;

                long currentStock = scanInventory(ctx, sinkDir, stack);
                if (currentStock < config.amount()) {
                    return RoutePlan.reroute(sinkDir);
                }
            }
        }

        // Items already addressed to this pipe still get delivered
        return super.route(ctx, item, options);
    }

    private long scanInventory(PipeContext ctx, Direction direction, ItemStack target) {
        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage =
                ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) return 0;

        ItemVariant targetVariant = ItemVariant.of(target);
        long count = 0;
        for (StorageView<ItemVariant> view : storage) {
            if (view.getResource().equals(targetVariant)) {
                count += view.getAmount();
            }
        }
        return count;
    }
}
