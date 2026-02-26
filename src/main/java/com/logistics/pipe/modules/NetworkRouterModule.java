package com.logistics.pipe.modules;

import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.PipeNetwork;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

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
    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) {
            return RoutePlan.pass(); // Networks only exist on server
        }

        PipeNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return RoutePlan.pass(); // Not in a network, fall back to default routing
        }

        // If item has no destination, try to find one
        if (item.getDestination() == null) {
            BlockPos destination = findDestinationForItem(ctx, item, network);
            if (destination != null) {
                item.setDestination(destination);
            } else {
                // No destination available - drop the item
                return RoutePlan.drop();
            }
        }

        // Route item to destination using pathfinding
        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            return RoutePlan.reroute(nextHop);
        }

        // Pathfinding failed - drop the item
        return RoutePlan.drop();
    }

    /**
     * Find a suitable destination for an item without an explicit destination.
     * Priority order:
     * 1. Requesters that want this item (highest priority - TODO: Phase 5+)
     * 2. Default route sinks that accept any item
     * 3. Any sink that can accept this item
     *
     * @return BlockPos of destination, or null if no destination found
     */
    @Nullable
    private BlockPos findDestinationForItem(PipeContext ctx, TravelingItem item, PipeNetwork network) {
        return network.findSinkFor(item.getStack());
    }
}
