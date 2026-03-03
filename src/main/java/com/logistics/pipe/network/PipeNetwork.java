package com.logistics.pipe.network;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
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
    private final RequestMatcher requestMatcher;

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
        this.requestMatcher = new RequestMatcher();
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
        this.requestMatcher = new RequestMatcher();
    }

    public UUID getId() {
        return id;
    }

    /**
     * Get short network ID for logging (first 8 characters).
     */
    static String getNetworkIdShort(UUID id) {
        return id.toString().substring(0, 8);
    }

    public void addPipe(BlockPos pos) {
        graph.addNode(pos);
    }

    public void removePipe(BlockPos pos) {
        graph.removeNode(pos);
        requestMatcher.removeProviderCache(pos);
        requestMatcher.removeOrdersFor(pos);
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
    @Override
    public void updateProviderCache(BlockPos pos, Map<ItemStack, Long> items, long gameTime) {
        requestMatcher.updateProviderCache(pos, items, gameTime);
    }

    /**
     * Get available amount of an item across all providers.
     */
    @Override
    public long getAvailableAmount(ItemStack stack) {
        return requestMatcher.getAvailableAmount(stack);
    }

    /**
     * Get all available items from all providers in the network.
     * Returns a map of ItemStack to total available amount.
     */
    @Override
    public Map<ItemStack, Long> getAllAvailableItems() {
        return requestMatcher.getAllAvailableItems();
    }

    /**
     * Add a request to the queue.
     */
    @Override
    public void addRequest(ItemRequest request) {
        requestMatcher.addRequest(request);
    }

    /**
     * Add an order for a provider to fulfill.
     */
    @Override
    public void addOrder(LogisticsOrder order) {
        requestMatcher.addOrder(order);
    }

    @Override
    public long getOrderedAmountFor(BlockPos requester, ItemStack stack) {
        return requestMatcher.getOrderedAmountFor(requester, net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack));
    }

    /**
     * Get pending orders for a specific provider.
     */
    @Override
    public List<LogisticsOrder> getOrdersFor(BlockPos provider) {
        return requestMatcher.getOrdersFor(provider);
    }

    /**
     * Remove a completed order.
     */
    @Override
    public void removeOrder(LogisticsOrder order) {
        requestMatcher.removeOrder(order);
    }

    /**
     * Transition a pending order to in-transit when items are shipped.
     */
    @Override
    public LogisticsOrder markShipped(LogisticsOrder order, long shippedAmount, long gameTime) {
        return requestMatcher.markShipped(order, shippedAmount, gameTime);
    }

    /**
     * Confirm physical delivery of an in-transit item to an inventory.
     */
    @Override
    public void confirmDelivery(LogisticsOrder order) {
        requestMatcher.confirmDelivery(order);
    }

    /**
     * Tick the network (process requests and clean up stale in-transit orders).
     */
    public void tick(long gameTime) {
        requestMatcher.processRequests(gameTime);
        requestMatcher.cleanupStaleInTransit(gameTime);
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

        LogisticsMod.LOGGER.debug("[Network {}] Finding sink for {} (members: {}, default routes: {})",
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
            if (worldView.matchesSinkFilter(pos, stack)) {
                LogisticsMod.LOGGER.debug("[Network {}] Found filtered sink at {}",
                        getNetworkIdShort(id), pos);
                return pos;
            }
        }

        return null;
    }

    /**
     * Find the highest-priority default route sink.
     * @return BlockPos of best default route sink, or null if none available
     */
    private BlockPos findDefaultRouteSink() {
        LogisticsMod.LOGGER.debug("[Network {}] No filtered sink, checking {} default routes",
                getNetworkIdShort(id), defaultRouteSinks.size());

        BlockPos bestSink = null;
        int bestPriority = Integer.MIN_VALUE;

        for (BlockPos sink : defaultRouteSinks) {
            if (!graph.contains(sink)) {
                continue;
            }

            int priority = sinkPriorities.getOrDefault(sink, DEFAULT_SINK_PRIORITY);
            if (priority > bestPriority) {
                LogisticsMod.LOGGER.debug("[Network {}] Found default route at {} (priority: {})",
                        getNetworkIdShort(id), sink, priority);
                bestSink = sink;
                bestPriority = priority;
            }
        }

        if (bestSink != null) {
            LogisticsMod.LOGGER.debug("[Network {}] Selected default route at {}",
                    getNetworkIdShort(id), bestSink);
        }

        return bestSink;
    }

    /**
     * Merge another network into this one.
     * For sink priorities, the higher value wins when both networks have the same position.
     */
    public void merge(PipeNetwork other) {
        graph.merge(other.graph);
        requestMatcher.merge(other.requestMatcher);
        defaultRouteSinks.addAll(other.defaultRouteSinks);
        other.sinkPriorities.forEach((pos, priority) ->
                sinkPriorities.merge(pos, priority, Math::max));
    }
}
