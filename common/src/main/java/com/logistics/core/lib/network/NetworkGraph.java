package com.logistics.core.lib.network;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Pure graph data structure and operations.
 * Zero Minecraft API coupling - 100% testable with pure Java.
 *
 * Manages graph topology, path finding, and path caching.
 */
public class NetworkGraph implements INetworkGraph {
    private final Set<BlockPos> nodes = new HashSet<>();
    private final Map<PathKey, CachedPath> pathCache = new HashMap<>();

    // Per-destination next-hop tables. The graph is unweighted, so one BFS from a destination yields
    // the next hop for every source toward it — shared across all items routing there, instead of a
    // per-(source, destination) A* search. LRU-bounded; invalidated wholesale on any topology change.
    private static final int NEXT_HOP_CACHE_MAX = 64;
    private final Map<BlockPos, Map<BlockPos, Direction>> nextHopCache =
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<BlockPos, Map<BlockPos, Direction>> eldest) {
                return size() > NEXT_HOP_CACHE_MAX;
            }
        };

    private static final int PATH_CACHE_MAX_AGE = 200;

    private record PathKey(BlockPos start, BlockPos end) {}

    private record CachedPath(List<BlockPos> path, long createdAt) {}

    @Override
    public void addNode(BlockPos pos) {
        if (nodes.add(pos)) {
            invalidatePathCache();
        }
    }

    @Override
    public void removeNode(BlockPos pos) {
        if (nodes.remove(pos)) {
            invalidatePathCache();
        }
    }

    @Override
    public boolean contains(BlockPos pos) {
        return nodes.contains(pos);
    }

    @Override
    public Set<BlockPos> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Override
    @Nullable
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        if (!nodes.contains(start) || !nodes.contains(goal)) {
            return null;
        }

        PathKey key = new PathKey(start, goal);
        CachedPath cached = pathCache.get(key);

        long currentTime = System.currentTimeMillis();
        if (cached != null && currentTime - cached.createdAt < PATH_CACHE_MAX_AGE) {
            return cached.path;
        }

        List<BlockPos> path = NetworkPathfinder.findPath(start, goal, nodes);
        if (path != null) {
            pathCache.put(key, new CachedPath(path, currentTime));
        }

        return path;
    }

    @Override
    @Nullable
    public Direction getNextHop(BlockPos current, BlockPos destination) {
        if (!nodes.contains(current) || !nodes.contains(destination)) {
            return null;
        }

        Map<BlockPos, Direction> table = nextHopCache.get(destination);
        if (table == null) {
            table = computeNextHopTable(destination);
            nextHopCache.put(destination, table);
        }
        return table.get(current);
    }

    /**
     * Breadth-first search outward from {@code goal} over the unweighted graph, recording for each
     * reachable node the direction of its first step along a shortest path back to {@code goal}.
     * One pass serves every source; {@code goal} itself has no entry.
     */
    private Map<BlockPos, Direction> computeNextHopTable(BlockPos goal) {
        Map<BlockPos, Direction> nextHop = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(goal);
        visited.add(goal);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!nodes.contains(neighbor) || !visited.add(neighbor)) {
                    continue;
                }
                // neighbor sits one step farther from goal; stepping back to current heads toward it.
                nextHop.put(neighbor, dir.getOpposite());
                queue.add(neighbor);
            }
        }
        return nextHop;
    }

    @Override
    public void merge(INetworkGraph other) {
        for (BlockPos pos : other.getNodes()) {
            addNode(pos);
        }
    }

    @Override
    public int size() {
        return nodes.size();
    }

    /**
     * Invalidate all cached routing data (paths and next-hop tables).
     * Called when graph topology changes (add/remove nodes).
     */
    private void invalidatePathCache() {
        pathCache.clear();
        nextHopCache.clear();
    }
}
