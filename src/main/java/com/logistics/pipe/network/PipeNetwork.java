package com.logistics.pipe.network;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.network.INetworkGraph;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a connected network of pipes.
 * Manages network state, pathfinding, and standing order dispatch.
 *
 * Implements ILogisticsNetwork to provide abstraction for modules.
 */
public class PipeNetwork implements ILogisticsNetwork {
    private final UUID id;
    private final INetworkGraph graph;
    private final IWorldView worldView;
    private final NetworkController controller;

    // Sink management for item routing
    private final Set<BlockPos> defaultRouteSinks = new HashSet<>(); // Accepts any item
    private final Map<BlockPos, Integer> sinkPriorities = new HashMap<>(); // Priority per sink (higher = better)

    private static final int DEFAULT_SINK_PRIORITY = 0;

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
        this.controller = new NetworkController();
        controller.setOrderFailureListener((orderId, requester, item, amount, missing) -> {
            if (worldView == null) return;
            String name = item.toStack(1).getHoverName().getString();
            String missingNames = missing.stream()
                    .map(v -> v.toStack(1).getHoverName().getString())
                    .distinct()
                    .collect(Collectors.joining(", "));
            worldView.broadcastAlert(requester,
                    Component.literal("[Logistics] Cannot fill order for "
                            + name + " — missing: " + missingNames));
        });
    }

    /**
     * Legacy constructor for tests.
     * Creates a PipeNetwork without IWorldView (will fail if sink/dispatch operations are used).
     * @deprecated Use constructor with IWorldView injection
     */
    @Deprecated
    public PipeNetwork(UUID id) {
        this.id = id;
        this.graph = new NetworkGraph();
        this.worldView = null;
        this.controller = new NetworkController();
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
        controller.removeSupply(pos);
        controller.cancelOrdersFor(pos);
        controller.unregisterProviderCheck(pos);
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
     */
    public Direction getNextHop(BlockPos current, BlockPos destination) {
        return graph.getNextHop(current, destination);
    }

    /**
     * Find path between two positions.
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        return graph.findPath(start, goal);
    }

    @Override
    public void registerSupply(BlockPos pos, Map<ItemVariant, Long> items, int priority) {
        controller.registerSupply(pos, items, priority);
    }

    @Override
    public long getAvailableAmount(ItemStack stack) {
        return controller.getAvailableAmount(ItemVariant.of(stack));
    }

    @Override
    public Map<ItemStack, Long> getAllAvailableItems() {
        return controller.getAllAvailableItems();
    }

    @Override
    public UUID placeOrder(ItemVariant item, long amount, BlockPos requester, FulfillmentMode fulfillmentMode) {
        return controller.placeOrder(item, amount, requester, fulfillmentMode);
    }

    @Override
    public void cancelOrder(UUID orderId) {
        controller.cancelOrder(orderId);
    }

    @Override
    public void registerProviderCheck(BlockPos pos, ProviderCanFulfill check) {
        controller.registerProviderCheck(pos, check);
    }

    @Override
    public void unregisterProviderCheck(BlockPos pos) {
        controller.unregisterProviderCheck(pos);
    }

    @Override
    public List<ItemVariant> getMissingIngredients(ItemVariant item, long amount) {
        return controller.getMissingIngredients(item, amount);
    }

    @Override
    public long getOrderedAmountFor(BlockPos requester, ItemStack stack) {
        return controller.getOrderedAmountFor(requester, ItemVariant.of(stack));
    }

    @Override
    public void notifyDelivery(BlockPos requester, ItemVariant item, long amount) {
        controller.notifyDelivery(requester, item, amount);
    }

    /**
     * Tick the network: dispatch all fulfillable standing orders synchronously.
     * Provider modules extract items and inject TravelingItems before returning.
     * Supply table is updated after each dispatch so subsequent orders see accurate stock.
     */
    public void tick(long gameTime) {
        if (worldView == null) return;
        NetworkController.DispatchCommand cmd;
        while ((cmd = controller.nextDispatchable()) != null) {
            NetDbg.out("[Network {}] Dispatch: {} | provider={} → requester={} | {}x {}",
                    getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8),
                    cmd.provider(), cmd.requester(), cmd.amount(), cmd.item().toStack().getItem());
            try {
                long shipped = worldView.dispatch(
                        cmd.provider(), cmd.requester(), cmd.item(), cmd.amount(), cmd.orderId());
                if (shipped > 0) {
                    NetDbg.out("[Network {}] Shipped: {} | {} items",
                            getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8), shipped);
                    controller.recordDispatched(cmd.orderId(), shipped);
                } else {
                    controller.markSupplyUnavailable(cmd.provider());
                }
            } catch (Exception e) {
                LogisticsMod.LOGGER.error("[Network {}] Dispatch failed for order {}: {}",
                        getNetworkIdShort(id), cmd.orderId(), e.getMessage(), e);
                controller.markSupplyUnavailable(cmd.provider());
            }
        }
    }

    /**
     * Register a sink that accepts any item (default route).
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
     * 1. Sinks with matching filters
     * 2. Default route sinks
     */
    @Override
    public BlockPos findSinkFor(ItemStack stack) {
        if (worldView == null) {
            throw new IllegalStateException("Cannot use findSinkFor without IWorldView - update tests to use proper constructor");
        }

        NetDbg.out("[Network {}] Finding sink for {} (members: {}, default routes: {})",
                getNetworkIdShort(id), stack.getItem(), graph.size(), defaultRouteSinks.size());

        BlockPos bestSink = findFilteredSink(stack);
        if (bestSink == null) {
            bestSink = findDefaultRouteSink();
        }

        if (bestSink == null) {
            NetDbg.out("[Network {}] No sink found for {}", getNetworkIdShort(id), stack.getItem());
        }

        return bestSink;
    }

    private BlockPos findFilteredSink(ItemStack stack) {
        for (BlockPos pos : graph.getNodes()) {
            if (worldView.matchesSinkFilter(pos, stack)) {
                NetDbg.out("[Network {}] Found filtered sink at {}",
                        getNetworkIdShort(id), pos);
                return pos;
            }
        }
        return null;
    }

    private BlockPos findDefaultRouteSink() {
        NetDbg.out("[Network {}] No filtered sink, checking {} default routes",
                getNetworkIdShort(id), defaultRouteSinks.size());

        BlockPos bestSink = null;
        int bestPriority = Integer.MIN_VALUE;

        for (BlockPos sink : defaultRouteSinks) {
            if (!graph.contains(sink)) continue;

            int priority = sinkPriorities.getOrDefault(sink, DEFAULT_SINK_PRIORITY);
            if (priority > bestPriority) {
                NetDbg.out("[Network {}] Found default route at {} (priority: {})",
                        getNetworkIdShort(id), sink, priority);
                bestSink = sink;
                bestPriority = priority;
            }
        }

        if (bestSink != null) {
            NetDbg.out("[Network {}] Selected default route at {}",
                    getNetworkIdShort(id), bestSink);
        }

        return bestSink;
    }

    /**
     * Merge another network into this one.
     */
    public void merge(PipeNetwork other) {
        graph.merge(other.graph);
        controller.merge(other.controller);
        defaultRouteSinks.addAll(other.defaultRouteSinks);
        other.sinkPriorities.forEach((pos, priority) ->
                sinkPriorities.merge(pos, priority, Math::max));
    }
}
