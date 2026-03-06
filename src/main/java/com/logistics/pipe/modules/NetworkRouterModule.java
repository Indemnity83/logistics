package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Module that enables network-aware routing for pipes.
 * Routes items with explicit destinations using A* pathfinding through the pipe network.
 * For items without destinations, attempts to find a suitable sink in the network.
 * Drops items if no destination can be found.
 *
 * <p>Add this module to pipes that should participate in smart routing (Provider, Requester, Routing pipes).
 * Regular transport pipes (copper, iron, etc.) should NOT have this module.
 */
public class NetworkRouterModule implements Module {
    // Accelerate aggressively so items reach ITEM_NETWORK_SPEED within one pipe segment.
    // ITEM_NETWORK_SPEED (0.2) - ITEM_MIN_SPEED (0.02) = 0.18 over ~5 ticks → 0.036/tick.
    private static final float NETWORK_ACCELERATION = 0.04f;

    @Override
    public float getAcceleration(PipeContext ctx) {
        return NETWORK_ACCELERATION;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsPipe.CONFIG.ITEM_NETWORK_SPEED;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network == null) return RoutePlan.pass();

        if (item.getDestination() == null) {
            BlockPos destination = network.findSinkFor(item.getStack());
            if (destination == null) return RoutePlan.drop();
            item.setDestination(destination);
        }

        if (item.getDestination().equals(ctx.pos())) return RoutePlan.pass();

        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            return RoutePlan.reroute(nextHop);
        }

        return RoutePlan.drop();
    }

}
