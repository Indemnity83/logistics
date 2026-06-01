package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.ItemStorageLookup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class InsertionModule implements Module, RoutingModule {
    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (options == null || options.isEmpty()) {
            return RoutePlan.drop();
        }

        List<InsertionRoutingPlanner.InventoryOption> inventoryOptions = new ArrayList<>();
        List<Direction> pipeDirections = new ArrayList<>();

        for (Direction direction : options) {
            if (ctx.isInventoryConnection(direction)) {
                inventoryOptions.add(new InsertionRoutingPlanner.InventoryOption(
                        direction, getInsertSpace(ctx, item, direction)));
                continue;
            }

            if (ctx.isNeighborPipe(direction)) {
                pipeDirections.add(direction);
            }
        }

        RoutePlan plan = InsertionRoutingPlanner.route(
                item,
                inventoryOptions,
                pipeDirections,
                InsertionRoutingPlanner.routeSeed(
                        ctx.pos().asLong(), ctx.world() == null ? 0L : ctx.world().getGameTime(), item.getDirection()));
        NetDbg.out("[Insertion @ {}] Routing {} → {}", ctx.pos(), item.getStack().getItem(), plan.getType());
        return plan;
    }

    private long getInsertSpace(PipeContext ctx, TravelingItem item, Direction direction) {
        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) {
            return 0;
        }

        IItemKey key = ItemStorageLookup.of(item.getStack());
        long amount = item.getStack().getCount();
        if (amount <= 0) {
            return 0;
        }

        long reserved = getRoutedAmount(ctx, direction, key);
        if (reserved < 0) {
            reserved = 0;
        }

        long requested = amount + reserved;
        long accepted = storage.insert(key, requested, true);
        long available = accepted - reserved;
        return Math.max(0, available);
    }

    private long getRoutedAmount(PipeContext ctx, Direction direction, IItemKey key) {
        long total = 0;
        for (TravelingItem other : ctx.blockEntity().getTravelingItems()) {
            if (!other.isRouted()) {
                continue;
            }

            if (other.getDirection() != direction) {
                continue;
            }

            IItemKey otherKey = ItemStorageLookup.of(other.getStack());
            if (!key.equals(otherKey)) {
                continue;
            }

            total += other.getStack().getCount();
        }
        return total;
    }

}
