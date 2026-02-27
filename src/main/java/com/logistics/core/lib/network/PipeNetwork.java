package com.logistics.core.lib.network;

import com.logistics.LogisticsMod;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SinkModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a connected network of pipes.
 * Manages network state, pathfinding, and request/order queues.
 */
public class PipeNetwork {
    private final UUID id;
    private final Set<BlockPos> members = new HashSet<>();
    private final Map<BlockPos, ProviderCache> providerCaches = new HashMap<>();
    private final List<ItemRequest> pendingRequests = new ArrayList<>();
    private final Map<BlockPos, List<LogisticsOrder>> pendingOrders = new HashMap<>();
    private final Map<PathKey, CachedPath> pathCache = new HashMap<>();

    // Sink management for item routing
    private final Set<BlockPos> defaultRouteSinks = new HashSet<>(); // Accepts any item
    private final Map<BlockPos, Integer> sinkPriorities = new HashMap<>(); // Priority per sink (higher = better)

    private static final int PATH_CACHE_MAX_AGE = 200; // 10 seconds
    private static final int DEFAULT_SINK_PRIORITY = 0;
    private static final int FILTERED_SINK_PRIORITY = 50; // Filtered sinks have higher priority than default routes
    private static final int REQUESTER_PRIORITY = 100; // Requesters have higher priority

    private record PathKey(BlockPos start, BlockPos end) {}

    private record CachedPath(List<BlockPos> path, long createdAt) {}

    public PipeNetwork(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Get short network ID for logging (first 8 characters).
     */
    private static String getNetworkIdShort(UUID id) {
        return id.toString().substring(0, 8);
    }

    public void addPipe(BlockPos pos) {
        if (members.add(pos)) {
            invalidatePathCache();
        }
    }

    public void removePipe(BlockPos pos) {
        if (members.remove(pos)) {
            providerCaches.remove(pos);
            pendingOrders.remove(pos);
            defaultRouteSinks.remove(pos);
            sinkPriorities.remove(pos);
            invalidatePathCache();
        }
    }

    public boolean contains(BlockPos pos) {
        return members.contains(pos);
    }

    public Set<BlockPos> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int size() {
        return members.size();
    }

    /**
     * Get next hop direction from current position toward destination.
     * Uses cached paths when available.
     */
    public Direction getNextHop(BlockPos current, BlockPos destination) {
        if (!members.contains(current) || !members.contains(destination)) {
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

    private static Direction directionFromDelta(int dx, int dy, int dz) {
        if (dx == 1) return Direction.EAST;
        if (dx == -1) return Direction.WEST;
        if (dy == 1) return Direction.UP;
        if (dy == -1) return Direction.DOWN;
        if (dz == 1) return Direction.SOUTH;
        if (dz == -1) return Direction.NORTH;
        return null;
    }

    /**
     * Find path between two positions, using cache when available.
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        PathKey key = new PathKey(start, goal);
        CachedPath cached = pathCache.get(key);

        long currentTime = System.currentTimeMillis();
        if (cached != null && currentTime - cached.createdAt < PATH_CACHE_MAX_AGE) {
            return cached.path;
        }

        List<BlockPos> path = NetworkPathfinder.findPath(start, goal, members);
        if (path != null) {
            pathCache.put(key, new CachedPath(path, currentTime));
        }

        return path;
    }

    /**
     * Update provider cache for a specific position.
     */
    public void updateProviderCache(BlockPos pos, Map<ItemStack, Long> items, long gameTime) {
        providerCaches.computeIfAbsent(pos, k -> new ProviderCache()).update(items, gameTime);
    }

    /**
     * Get available amount of an item across all providers.
     */
    public long getAvailableAmount(ItemStack stack) {
        long total = 0;
        for (ProviderCache cache : providerCaches.values()) {
            total += cache.getAvailableAmount(stack);
        }
        return total;
    }

    /**
     * Get all available items from all providers in the network.
     * Returns a map of ItemStack to total available amount.
     */
    public Map<ItemStack, Long> getAllAvailableItems() {
        Map<ItemStack, Long> aggregated = new HashMap<>();

        for (ProviderCache cache : providerCaches.values()) {
            for (Map.Entry<ItemStack, Long> entry : cache.getAvailableItems().entrySet()) {
                ItemStack stack = entry.getKey();
                long amount = entry.getValue();

                // Find matching stack in aggregated map
                boolean found = false;
                for (Map.Entry<ItemStack, Long> existing : aggregated.entrySet()) {
                    if (ItemStack.isSameItemSameComponents(existing.getKey(), stack)) {
                        existing.setValue(existing.getValue() + amount);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    aggregated.put(stack.copy(), amount);
                }
            }
        }

        return aggregated;
    }

    /**
     * Find provider position that has the requested item.
     * Returns null if no provider has sufficient quantity.
     */
    public BlockPos findProviderFor(ItemStack stack, long amount) {
        for (Map.Entry<BlockPos, ProviderCache> entry : providerCaches.entrySet()) {
            if (entry.getValue().getAvailableAmount(stack) >= amount) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Add a request to the queue.
     */
    public void addRequest(ItemRequest request) {
        pendingRequests.add(request);
    }

    /**
     * Add an order for a specific provider.
     */
    public void addOrder(LogisticsOrder order) {
        pendingOrders.computeIfAbsent(order.provider(), k -> new ArrayList<>()).add(order);
    }

    /**
     * Get pending orders for a specific provider.
     */
    public List<LogisticsOrder> getOrdersFor(BlockPos provider) {
        return pendingOrders.getOrDefault(provider, Collections.emptyList());
    }

    /**
     * Remove a completed order.
     */
    public void removeOrder(LogisticsOrder order) {
        List<LogisticsOrder> orders = pendingOrders.get(order.provider());
        if (orders != null) {
            orders.remove(order);
        }
    }

    /**
     * Process pending requests and create orders.
     * Called during network tick.
     */
    public void processRequests(long gameTime) {
        Iterator<ItemRequest> iterator = pendingRequests.iterator();
        while (iterator.hasNext()) {
            ItemRequest request = iterator.next();

            BlockPos provider = findProviderFor(request.stack(), request.amount());
            if (provider != null) {
                LogisticsOrder order = new LogisticsOrder(
                    provider,
                    request.requester(),
                    request.stack(),
                    request.amount(),
                    gameTime
                );
                addOrder(order);
                iterator.remove();
            }
        }
    }

    /**
     * Tick the network (process requests, clean up stale cache entries).
     */
    public void tick(long gameTime) {
        processRequests(gameTime);
        cleanupStaleCache(gameTime);
    }

    private void cleanupStaleCache(long gameTime) {
        pathCache.entrySet().removeIf(entry ->
            gameTime - entry.getValue().createdAt > PATH_CACHE_MAX_AGE
        );
    }

    private void invalidatePathCache() {
        pathCache.clear();
    }

    /**
     * Register a sink that accepts any item (default route).
     * @param sink BlockPos of the sink pipe
     * @param priority Priority level (higher values = preferred, default = 0)
     */
    public void registerDefaultRouteSink(BlockPos sink, int priority) {
        defaultRouteSinks.add(sink);
        sinkPriorities.put(sink, priority);
    }

    /**
     * Unregister a default route sink.
     */
    public void unregisterDefaultRouteSink(BlockPos sink) {
        defaultRouteSinks.remove(sink);
        sinkPriorities.remove(sink);
    }

    /**
     * Find a suitable destination for an item.
     * Priority order:
     * 1. Requesters that want this specific item (handled via pendingRequests)
     * 2. Sinks with matching filters (FILTERED_SINK_PRIORITY)
     * 3. Default route sinks (DEFAULT_SINK_PRIORITY)
     *
     * @param world Level to query pipe entities
     * @param stack ItemStack to find destination for
     * @return BlockPos of highest-priority available sink, or null if none found
     */
    public BlockPos findSinkFor(Level world, ItemStack stack) {
        LogisticsMod.LOGGER.info("[Network {}] Finding sink for {} (members: {}, default routes: {})",
                getNetworkIdShort(id), stack.getItem(), members.size(), defaultRouteSinks.size());

        BlockPos bestSink = null;
        int bestPriority = Integer.MIN_VALUE;

        // Check all network members for SinkModules with matching filters
        for (BlockPos pos : members) {
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
                Pipe pipe = block.getPipe();
                SinkModule sinkModule = pipe.getModule(SinkModule.class);

                if (sinkModule != null) {
                    PipeContext ctx = pipeEntity.createContext();

                    // Check if this sink has a matching filter
                    if (sinkModule.matchesFilter(ctx, stack)) {
                        int priority = FILTERED_SINK_PRIORITY;
                        if (priority > bestPriority) {
                            bestSink = pos;
                            bestPriority = priority;
                            LogisticsMod.LOGGER.info("[Network {}] Found filtered sink at {} (priority: {})",
                                    getNetworkIdShort(id), pos, priority);
                        }
                    }
                }
            }
        }

        // If no filtered sink found, check default route sinks
        if (bestSink == null) {
            LogisticsMod.LOGGER.info("[Network {}] No filtered sink, checking {} default routes",
                    getNetworkIdShort(id), defaultRouteSinks.size());
            for (BlockPos sink : defaultRouteSinks) {
                LogisticsMod.LOGGER.info("[Network {}] Checking default route at {} (in members: {})",
                        getNetworkIdShort(id), sink, members.contains(sink));
                int priority = sinkPriorities.getOrDefault(sink, DEFAULT_SINK_PRIORITY);
                if (priority > bestPriority && members.contains(sink)) {
                    bestSink = sink;
                    bestPriority = priority;
                    LogisticsMod.LOGGER.info("[Network {}] Selected default route at {} (priority: {})",
                            getNetworkIdShort(id), sink, priority);
                }
            }
        }

        if (bestSink == null) {
            LogisticsMod.LOGGER.warn("[Network {}] No sink found for {}", getNetworkIdShort(id), stack.getItem());
        }

        return bestSink;
    }

    /**
     * Merge another network into this one.
     */
    public void merge(PipeNetwork other) {
        members.addAll(other.members);
        providerCaches.putAll(other.providerCaches);
        pendingRequests.addAll(other.pendingRequests);

        for (Map.Entry<BlockPos, List<LogisticsOrder>> entry : other.pendingOrders.entrySet()) {
            pendingOrders.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }

        defaultRouteSinks.addAll(other.defaultRouteSinks);
        sinkPriorities.putAll(other.sinkPriorities);

        invalidatePathCache();
    }
}
