package com.logistics.core.lib.network;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeContext;
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
 *
 * Implements ILogisticsNetwork to provide abstraction for modules.
 */
public class PipeNetwork implements ILogisticsNetwork {
    private final UUID id;
    private final INetworkGraph graph;
    private final IWorldView worldView;
    private final Map<BlockPos, ProviderCache> providerCaches = new HashMap<>();
    private final List<ItemRequest> pendingRequests = new ArrayList<>();
    private final Map<BlockPos, List<LogisticsOrder>> pendingOrders = new HashMap<>();

    // Sink management for item routing
    private final Set<BlockPos> defaultRouteSinks = new HashSet<>(); // Accepts any item
    private final Map<BlockPos, Integer> sinkPriorities = new HashMap<>(); // Priority per sink (higher = better)

    private static final int DEFAULT_SINK_PRIORITY = 0;
    private static final int FILTERED_SINK_PRIORITY = 50; // Filtered sinks have higher priority than default routes
    private static final int REQUESTER_PRIORITY = 100; // Requesters have higher priority

    /**
     * Constructor with dependency injection.
     * @param id Network UUID
     * @param graph Graph implementation
     * @param worldView World query abstraction
     */
    public PipeNetwork(UUID id, INetworkGraph graph, IWorldView worldView) {
        this.id = id;
        this.graph = graph;
        this.worldView = worldView;
    }

    /**
     * Legacy constructor for tests.
     * Creates a PipeNetwork without IWorldView (will fail if sink operations are used).
     * @deprecated Use constructor with IWorldView injection
     */
    @Deprecated
    public PipeNetwork(UUID id) {
        this.id = id;
        this.graph = new NetworkGraph();
        this.worldView = null; // Tests will need to be updated to provide this
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
        graph.addNode(pos);
    }

    public void removePipe(BlockPos pos) {
        graph.removeNode(pos);
        providerCaches.remove(pos);
        pendingOrders.remove(pos);
        defaultRouteSinks.remove(pos);
        sinkPriorities.remove(pos);
    }

    public boolean contains(BlockPos pos) {
        return graph.contains(pos);
    }

    public Set<BlockPos> getMembers() {
        return graph.getNodes();
    }

    public int size() {
        return graph.size();
    }

    /**
     * Get next hop direction from current position toward destination.
     * Uses cached paths when available.
     */
    public Direction getNextHop(BlockPos current, BlockPos destination) {
        return graph.getNextHop(current, destination);
    }

    /**
     * Find path between two positions, using cache when available.
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        return graph.findPath(start, goal);
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
                addToAggregatedItems(aggregated, entry.getKey(), entry.getValue());
            }
        }

        return aggregated;
    }

    /**
     * Add an item amount to the aggregated map, combining with existing amounts
     * for items that are the same (same item and components).
     */
    private void addToAggregatedItems(Map<ItemStack, Long> aggregated, ItemStack stack, long amount) {
        // Try to find existing matching stack
        for (Map.Entry<ItemStack, Long> existing : aggregated.entrySet()) {
            if (ItemStack.isSameItemSameComponents(existing.getKey(), stack)) {
                existing.setValue(existing.getValue() + amount);
                return;
            }
        }

        // No match found - add new entry
        aggregated.put(stack.copy(), amount);
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
     * Tick the network (process requests).
     */
    public void tick(long gameTime) {
        processRequests(gameTime);
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
     * @param stack ItemStack to find destination for
     * @return BlockPos of highest-priority available sink, or null if none found
     */
    @Override
    public BlockPos findSinkFor(ItemStack stack) {
        if (worldView == null) {
            throw new IllegalStateException("Cannot use findSinkFor without IWorldView - update tests to use proper constructor");
        }

        LogisticsMod.LOGGER.info("[Network {}] Finding sink for {} (members: {}, default routes: {})",
                getNetworkIdShort(id), stack.getItem(), graph.size(), defaultRouteSinks.size());

        // Check filtered sinks first, then fall back to default routes
        BlockPos bestSink = findFilteredSink(stack);
        if (bestSink == null) {
            bestSink = findDefaultRouteSink();
        }

        if (bestSink == null) {
            LogisticsMod.LOGGER.warn("[Network {}] No sink found for {}", getNetworkIdShort(id), stack.getItem());
        }

        return bestSink;
    }

    /**
     * Find a sink with a matching filter for the given item.
     * Uses injected IWorldView to query modules.
     * @param stack ItemStack to find sink for
     * @return BlockPos of filtered sink, or null if none matches
     */
    private BlockPos findFilteredSink(ItemStack stack) {
        for (BlockPos pos : graph.getNodes()) {
            SinkModule sinkModule = worldView.getModule(pos, SinkModule.class);
            if (sinkModule == null) {
                continue;
            }

            // TODO: Need to get PipeContext somehow - for now we need to access Level
            // This is a temporary bridge until we refactor module API
            if (worldView instanceof MinecraftWorldView minecraftView) {
                Level world = minecraftView.getLevel();
                if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                    PipeContext ctx = pipeEntity.createContext();
                    if (sinkModule.matchesFilter(ctx, stack)) {
                        LogisticsMod.LOGGER.info("[Network {}] Found filtered sink at {}",
                                getNetworkIdShort(id), pos);
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find the highest-priority default route sink.
     * @return BlockPos of best default route sink, or null if none available
     */
    private BlockPos findDefaultRouteSink() {
        LogisticsMod.LOGGER.info("[Network {}] No filtered sink, checking {} default routes",
                getNetworkIdShort(id), defaultRouteSinks.size());

        BlockPos bestSink = null;
        int bestPriority = Integer.MIN_VALUE;

        for (BlockPos sink : defaultRouteSinks) {
            if (!graph.contains(sink)) {
                continue;
            }

            int priority = sinkPriorities.getOrDefault(sink, DEFAULT_SINK_PRIORITY);
            if (priority > bestPriority) {
                LogisticsMod.LOGGER.info("[Network {}] Found default route at {} (priority: {})",
                        getNetworkIdShort(id), sink, priority);
                bestSink = sink;
                bestPriority = priority;
            }
        }

        if (bestSink != null) {
            LogisticsMod.LOGGER.info("[Network {}] Selected default route at {}",
                    getNetworkIdShort(id), bestSink);
        }

        return bestSink;
    }

    /**
     * Merge another network into this one.
     */
    public void merge(PipeNetwork other) {
        graph.merge(other.graph);
        providerCaches.putAll(other.providerCaches);
        pendingRequests.addAll(other.pendingRequests);

        for (Map.Entry<BlockPos, List<LogisticsOrder>> entry : other.pendingOrders.entrySet()) {
            pendingOrders.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }

        defaultRouteSinks.addAll(other.defaultRouteSinks);
        sinkPriorities.putAll(other.sinkPriorities);
    }
}
