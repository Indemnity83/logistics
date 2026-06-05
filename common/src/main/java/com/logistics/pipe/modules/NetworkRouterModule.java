package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.LogisticsConfig;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
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
public class NetworkRouterModule implements Module, RoutingModule {
    // Accelerate aggressively so items reach ITEM_NETWORK_SPEED within one pipe segment.
    // ITEM_NETWORK_SPEED (0.2) - ITEM_MIN_SPEED (0.02) = 0.18 over ~5 ticks → 0.036/tick.
    private static final float NETWORK_ACCELERATION = 0.04f;
    // Energy cost to assign a destination for an unrouted item.
    private static final long RF_PER_ROUTE = 2;

    @Override
    public float getAcceleration(PipeContext ctx) {
        return NETWORK_ACCELERATION;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsConfig.get().pipe.injectSpeed;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        ILogisticsNetwork network = ctx.network();
        if (network == null) return RoutePlan.pass();

        if (item.getDestination() == null) {
            if (!network.consumeEnergy(RF_PER_ROUTE)) return RoutePlan.drop();
            BlockPos destination = network.findSinkFor(item.getStack());
            if (destination == null) {
                NetDbg.out("[NetworkRouter @ {}] No sink found for {}, dropping", ctx.pos(), item.getStack().getItem());
                return RoutePlan.drop();
            }
            item.setDestination(destination);
            NetDbg.out("[NetworkRouter @ {}] Assigned destination {} for {}", ctx.pos(), destination, item.getStack().getItem());
        }

        if (item.getDestination().equals(ctx.pos())) return RoutePlan.pass();

        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            NetDbg.out("[NetworkRouter @ {}] Routing {} → {} via {}", ctx.pos(), item.getStack().getItem(), item.getDestination(), nextHop);
            return RoutePlan.reroute(nextHop);
        }

        NetDbg.out("[NetworkRouter @ {}] No valid hop for {} → {} (nextHop={}, options={}), dropping", ctx.pos(), item.getStack().getItem(), item.getDestination(), nextHop, options);
        return RoutePlan.drop();
    }

}
