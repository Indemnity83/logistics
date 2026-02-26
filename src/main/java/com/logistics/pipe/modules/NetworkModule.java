package com.logistics.pipe.modules;

import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.PipeNetwork;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Module that enables network-aware routing for pipes.
 * Routes items with explicit destinations using A* pathfinding through the pipe network.
 * Falls back to random routing for items without destinations.
 *
 * <p>Add this module to pipes that should participate in smart routing (Provider, Requester, Routing pipes).
 * Regular transport pipes (copper, iron, etc.) should NOT have this module.
 */
public class NetworkModule implements Module {
    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        // Try network routing if item has destination
        if (item.getDestination() != null) {
            RoutePlan networkPlan = tryNetworkRouting(ctx, item, options);
            if (networkPlan != null) {
                return networkPlan;
            }
        }

        // No destination or network routing failed - use default random routing
        return RoutePlan.pass();
    }

    /**
     * Attempt to route item using network pathfinding.
     * @return RoutePlan if network routing succeeds, null otherwise
     */
    @Nullable
    private RoutePlan tryNetworkRouting(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) {
            return null; // Networks only exist on server
        }

        PipeNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return null;
        }

        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            return RoutePlan.reroute(nextHop);
        }

        return null; // Network routing failed
    }
}
