package com.logistics.core.lib.network;

import net.minecraft.core.BlockPos;

/**
 * Routed distance between two nodes of a pipe network, measured in pipe hops.
 *
 * <p>A one-method view of {@link INetworkGraph#hopDistance} so that selection code
 * (sink resolution, provider dispatch) depends on distance alone rather than on the
 * whole graph.
 */
@FunctionalInterface
public interface HopDistance {

    /** Distance reported for a pair with no route between them. */
    int UNREACHABLE = Integer.MAX_VALUE;

    /**
     * Length of the shortest route from {@code from} to {@code to}, counted in edges:
     * {@code 0} for the same node, {@code 1} for adjacent nodes, {@link #UNREACHABLE}
     * when no route exists.
     */
    int hops(BlockPos from, BlockPos to);

    /**
     * Oracle that knows no routes at all — every pair is {@link #UNREACHABLE}, which makes
     * the distance tier of {@link RoutingPreference} a no-op. Used where no graph is
     * available (legacy constructors, focused unit tests).
     */
    HopDistance UNKNOWN = (from, to) -> UNREACHABLE;
}
