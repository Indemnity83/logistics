package com.logistics.core.lib.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private static final int PATH_CACHE_MAX_AGE = 200; // 10 seconds (in milliseconds)

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

        List<BlockPos> path = findPath(current, destination);
        if (path == null || path.size() < 2) {
            return null;
        }

        BlockPos nextPos = path.get(1);
        return directionFromDelta(
            nextPos.getX() - current.getX(),
            nextPos.getY() - current.getY(),
            nextPos.getZ() - current.getZ()
        );
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
     * Invalidate all cached paths.
     * Called when graph topology changes (add/remove nodes).
     */
    private void invalidatePathCache() {
        pathCache.clear();
    }

    /**
     * Convert position delta to Direction.
     */
    @Nullable
    private static Direction directionFromDelta(int dx, int dy, int dz) {
        if (dx == 1) return Direction.EAST;
        if (dx == -1) return Direction.WEST;
        if (dy == 1) return Direction.UP;
        if (dy == -1) return Direction.DOWN;
        if (dz == 1) return Direction.SOUTH;
        if (dz == -1) return Direction.NORTH;
        return null;
    }
}
