package com.logistics.core.lib.network;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Pure graph data structure and operations.
 * Zero Minecraft dependencies - 100% testable.
 */
public interface INetworkGraph {
    /**
     * Add a node to the graph.
     */
    void addNode(BlockPos pos);

    /**
     * Remove a node from the graph.
     */
    void removeNode(BlockPos pos);

    /**
     * Check if graph contains a node.
     */
    boolean contains(BlockPos pos);

    /**
     * Get all nodes in the graph.
     */
    Set<BlockPos> getNodes();

    /**
     * Find shortest path between two nodes.
     * Uses NetworkPathfinder internally.
     *
     * @return List of positions from start to goal (inclusive), or null if no path exists
     */
    @Nullable
    List<BlockPos> findPath(BlockPos start, BlockPos goal);

    /**
     * Get next hop direction from current toward destination.
     *
     * @return Direction to travel, or null if no path exists
     */
    @Nullable
    Direction getNextHop(BlockPos current, BlockPos destination);

    /**
     * Shortest routed distance between two nodes, counted in pipe hops (edges, not nodes):
     * {@code 0} for the same node, {@code 1} for adjacent nodes.
     *
     * @return hop count, or {@link HopDistance#UNREACHABLE} when either node is absent from
     *         the graph or no route connects them
     */
    int hopDistance(BlockPos from, BlockPos to);

    /**
     * Merge another graph into this one.
     */
    void merge(INetworkGraph other);

    /**
     * Get the number of nodes in the graph.
     */
    int size();
}
