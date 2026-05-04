package com.logistics.core.lib.network;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* algorithm pathfinding for pipe networks.
 * Finds shortest path between two positions using Manhattan distance heuristic.
 */
public class NetworkPathfinder {
    private static final double MOVE_COST = 1.0;

    private static class Node implements Comparable<Node> {
        final BlockPos pos;
        final double gCost; // Actual cost from start
        final double hCost; // Estimated cost to goal (heuristic)
        final Node parent;

        Node(BlockPos pos, double gCost, double hCost, Node parent) {
            this.pos = pos;
            this.gCost = gCost;
            this.hCost = hCost;
            this.parent = parent;
        }

        double fCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost(), other.fCost());
        }
    }

    /**
     * Find shortest path from start to goal through network members.
     * @param start Starting position
     * @param goal Goal position
     * @param networkMembers All pipes in the network
     * @return List of positions from start to goal (inclusive), or null if no path exists
     */
    public static List<BlockPos> findPath(BlockPos start, BlockPos goal, Set<BlockPos> networkMembers) {
        if (!networkMembers.contains(start) || !networkMembers.contains(goal)) {
            return null;
        }

        if (start.equals(goal)) {
            return List.of(start);
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<BlockPos, Double> bestCosts = new HashMap<>();

        openSet.add(new Node(start, 0, manhattanDistance(start, goal), null));
        bestCosts.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.pos.equals(goal)) {
                return reconstructPath(current);
            }

            evaluateNeighbors(current, goal, networkMembers, openSet, bestCosts);
        }

        return null; // No path found
    }

    /**
     * Evaluate all neighbors of the current node and add them to the open set if they offer a better path.
     */
    private static void evaluateNeighbors(
        Node current,
        BlockPos goal,
        Set<BlockPos> networkMembers,
        PriorityQueue<Node> openSet,
        Map<BlockPos, Double> bestCosts
    ) {
        for (BlockPos neighbor : getNeighbors(current.pos)) {
            if (!networkMembers.contains(neighbor)) {
                continue;
            }

            updatePathIfBetter(current, neighbor, goal, openSet, bestCosts);
        }
    }

    /**
     * Update the path to a neighbor if the new route offers a lower cost.
     */
    private static void updatePathIfBetter(
        Node current,
        BlockPos neighbor,
        BlockPos goal,
        PriorityQueue<Node> openSet,
        Map<BlockPos, Double> bestCosts
    ) {
        double newGCost = current.gCost + MOVE_COST;
        Double existingCost = bestCosts.get(neighbor);

        if (existingCost == null || newGCost < existingCost) {
            bestCosts.put(neighbor, newGCost);
            double hCost = manhattanDistance(neighbor, goal);
            openSet.add(new Node(neighbor, newGCost, hCost, current));
        }
    }

    private static List<BlockPos> reconstructPath(Node goal) {
        List<BlockPos> path = new ArrayList<>();
        Node current = goal;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static double manhattanDistance(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
             + Math.abs(a.getY() - b.getY())
             + Math.abs(a.getZ() - b.getZ());
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return List.of(
            pos.north(),
            pos.south(),
            pos.east(),
            pos.west(),
            pos.above(),
            pos.below()
        );
    }
}
