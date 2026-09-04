package com.logistics.pipe.network;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPipe;
import com.logistics.core.LogisticsProfiler;
import com.logistics.core.lib.network.*;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.logistics.core.lib.energy.IEnergyStorage;

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
    private final FluidOrderBook fluidOrderBook = new FluidOrderBook();
    private final JobCoordinator jobCoordinator;
    private final SinkResolver sinkResolver;

    // Satellite registry: logical ID → pipe position
    private final Map<String, BlockPos> satellites = new HashMap<>();

    // Battery positions registered as energy sources. Insertion order = draw order.
    // Storage is resolved from the world on demand so removed/unloaded batteries are skipped.
    private final Set<BlockPos> energySources = new LinkedHashSet<>();

    // Per-tick cache for isPowered(): every logistics pipe queries the shared network each tick,
    // so the source scan is computed once per game tick and reused for the rest of that tick.
    private long lastPoweredCheckTick = Long.MIN_VALUE;
    private boolean cachedPowered;

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
        this.jobCoordinator = new JobCoordinator(controller);
        this.sinkResolver = new SinkResolver(graph, worldView);
        controller.setOrderFailureListener((orderId, requester, item, amount, missing) -> {
            // Notify job coordinator first so job state is updated before the alert fires
            jobCoordinator.onOrderFailed(orderId, requester, item, amount, missing);
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
        this.jobCoordinator = new JobCoordinator(controller);
        this.sinkResolver = new SinkResolver(graph, null);
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
        controller.unregisterCrafterSnapshot(pos);
        fluidOrderBook.removeSupply(pos);
        fluidOrderBook.cancelOrdersFor(pos);
        sinkResolver.remove(pos);
        satellites.entrySet().removeIf(e -> pos.equals(e.getValue()));
    }

    public boolean contains(BlockPos pos) {
        return graph.contains(pos);
    }

    public java.util.Set<BlockPos> getMembers() {
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
        LogisticsProfiler.push("pathfind");
        try {
            return graph.findPath(start, goal);
        } finally {
            LogisticsProfiler.pop();
        }
    }

    @Override
    public void registerSupply(BlockPos pos, Map<IItemKey, Long> items, int priority) {
        controller.registerSupply(pos, items, priority);
    }

    @Override
    public long getAvailableAmount(ItemStack stack) {
        return controller.getAvailableAmount(ItemStorageLookup.of(stack));
    }

    @Override
    public Map<ItemStack, Long> getAllAvailableItems() {
        return controller.getAllAvailableItems();
    }

    @Override
    public UUID placeOrder(IItemKey item, long amount, BlockPos requester, FulfillmentMode fulfillmentMode) {
        return controller.placeOrder(item, amount, requester, fulfillmentMode);
    }

    @Override
    public void cancelOrder(UUID orderId) {
        controller.cancelOrder(orderId);
    }

    @Override
    public void registerCrafterSnapshot(BlockPos pos, CrafterSnapshot snapshot) {
        controller.registerCrafterSnapshot(pos, snapshot);
    }

    @Override
    public void unregisterCrafterSnapshot(BlockPos pos) {
        controller.unregisterCrafterSnapshot(pos);
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
    public List<IItemKey> getMissingIngredients(IItemKey item, long amount) {
        return controller.getMissingIngredients(item, amount);
    }

    @Override
    public long getOrderedAmountFor(BlockPos requester, ItemStack stack) {
        return controller.getOrderedAmountFor(requester, ItemStorageLookup.of(stack));
    }

    /**
     * If {@code item} is a fluid packet, returns its carried {@link FluidPacket} data; otherwise
     * {@code null}. Used to redirect generic item-shaped delivery/failure notifications (from
     * {@code PipeRuntime}'s drop/TTL-expiry paths, which don't know fluid packets are special) to the
     * fluid bookkeeping path instead of the item path — never both, since a fluid packet must never be
     * double-acknowledged.
     */
    @Nullable
    private static FluidPacket asFluidPacket(IItemKey item) {
        ItemStack sample = item.toStack(1);
        return sample.is(LogisticsPipe.ITEM.FLUID_PACKET) ? sample.get(LogisticsPipe.DATA.FLUID_PACKET) : null;
    }

    @Override
    public void notifyDelivery(BlockPos requester, IItemKey item, long amount) {
        // No fluid bridge here: fluid packets are only ever acknowledged through the deliveryId-tracked
        // overloads below (every packet this network mints carries one — see FluidProviderModule), so
        // this no-id path is never reached for a fluid packet. If it somehow were, falling through to
        // the item path is a harmless no-op — fluid packets never register order state under IItemKey.
        controller.notifyDelivery(requester, item, amount);
        jobCoordinator.onDelivery(requester, item, amount);
    }

    @Override
    public void notifyDelivery(UUID deliveryId, BlockPos requester, IItemKey item, long amount) {
        FluidPacket data = asFluidPacket(item);
        if (data != null) {
            long itemCount = amount; // packets in the stack, never mB directly — convert below.
            notifyFluidDelivery(deliveryId, requester, data.fluid(), itemCount * data.amountMb());
            return;
        }
        controller.notifyDelivery(deliveryId, requester, item, amount);
        jobCoordinator.onDelivery(requester, item, amount);
    }

    @Override
    public void notifyDeliveryFailed(UUID deliveryId, BlockPos requester, IItemKey item, long amount) {
        FluidPacket data = asFluidPacket(item);
        if (data != null) {
            long itemCount = amount; // packets in the stack, never mB directly — convert below.
            notifyFluidDeliveryFailed(deliveryId, requester, data.fluid(), itemCount * data.amountMb());
            return;
        }
        UUID replacementOrderId = controller.notifyDeliveryFailed(deliveryId, requester, item, amount);
        jobCoordinator.onDeliveryFailed(deliveryId, replacementOrderId);
    }

    @Override
    public void notifyDeliveryFailedNoId(BlockPos requester, IItemKey item, long amount) {
        FluidPacket data = asFluidPacket(item);
        if (data != null) {
            long itemCount = amount; // packets in the stack, never mB directly — convert below.
            notifyFluidDeliveryFailedNoId(requester, data.fluid(), itemCount * data.amountMb());
            return;
        }
        controller.notifyDeliveryFailedNoId(requester, item, amount);
    }

    // ===== Fluid Order Operations =====

    @Override
    public void registerFluidSupply(BlockPos pos, Fluid fluid, long availableMb, int priority) {
        fluidOrderBook.registerSupply(pos, fluid, availableMb, priority);
    }

    @Override
    public void removeFluidSupply(BlockPos pos) {
        fluidOrderBook.removeSupply(pos);
    }

    @Override
    public UUID placeFluidOrder(Fluid fluid, long amountMb, BlockPos requester, FulfillmentMode fulfillmentMode) {
        return fluidOrderBook.placeOrder(fluid, amountMb, requester, fulfillmentMode);
    }

    @Override
    public void cancelFluidOrder(UUID orderId) {
        fluidOrderBook.cancelOrder(orderId);
    }

    @Override
    public void cancelFluidOrdersFor(BlockPos requester) {
        fluidOrderBook.cancelOrdersFor(requester);
    }

    @Override
    public long getFluidOrderedMbFor(BlockPos requester, Fluid fluid) {
        return fluidOrderBook.getOrderedAmountFor(requester, fluid);
    }

    @Override
    public void notifyFluidDelivery(UUID deliveryId, BlockPos requester, Fluid fluid, long amountMb) {
        fluidOrderBook.notifyDelivery(deliveryId, requester, fluid, amountMb);
    }

    @Override
    public void notifyFluidDeliveryFailed(UUID deliveryId, BlockPos requester, Fluid fluid, long amountMb) {
        fluidOrderBook.notifyDeliveryFailed(deliveryId, requester, fluid, amountMb);
    }

    @Override
    public void notifyFluidDeliveryFailedNoId(BlockPos requester, Fluid fluid, long amountMb) {
        fluidOrderBook.notifyDeliveryFailedNoId(requester, fluid, amountMb);
    }

    /**
     * Tick the network: dispatch all fulfillable standing orders synchronously.
     * Provider modules extract items and inject TravelingItems before returning.
     * Supply table is updated after each dispatch so subsequent orders see accurate stock.
     */
    public void tick(long gameTime) {
        if (worldView == null) return;
        LogisticsProfiler.push("network_dispatch");
        try {
            tickDispatch();
            tickFluidDispatch();
        } finally {
            LogisticsProfiler.pop();
        }
    }

    private void tickDispatch() {
        controller.clearDeferredProviders();
        NetworkCommandExecutor executor = new NetworkCommandExecutor(worldView);
        NetworkController.DispatchCommand cmd;
        while ((cmd = controller.nextDispatchable()) != null) {
            NetDbg.out("[Network {}] Dispatch: {} | provider={} → requester={} | {}x {}",
                    getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8),
                    cmd.provider(), cmd.requester(), cmd.amount(), cmd.item().toStack(1).getItem());
            try {
                CommandResult result = executor.execute(new NetworkCommand.ExtractCommand(
                        cmd.provider(), cmd.requester(), cmd.item(), cmd.amount(), cmd.orderId()));
                if (result.isSuccess()) {
                    NetDbg.out("[Network {}] Shipped: {} | {} items",
                            getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8),
                            result.itemsHandled());
                    controller.recordDispatched(cmd.orderId(), result.itemsHandled());
                } else if (result.isDeferred()) {
                    // Provider temporarily unavailable (e.g. crafter buffer full): skip this
                    // provider for the rest of this tick but leave supply in the table so the
                    // item remains visible in the network UI. Nothing shipped, so the reservation
                    // tryDispatch took has to go back before the order is retried.
                    controller.releaseReservations(cmd.orderId());
                    controller.deferProvider(cmd.provider());
                } else {
                    controller.markSupplyUnavailable(cmd.provider());
                }
            } catch (Exception e) {
                LogisticsMod.LOGGER.error("[Network {}] Dispatch failed for order {}: {}",
                        getNetworkIdShort(id), cmd.orderId(), e.getMessage(), e);
                controller.markSupplyUnavailable(cmd.provider());
            }
        }
        jobCoordinator.tick(controller);
    }

    /**
     * Tick fluid dispatch, mirroring {@link #tickDispatch()}'s try/catch and unavailable-supply
     * handling. {@code recordDispatched} is always called exactly once per attempt (even {@code 0} or
     * on a thrown exception) so a provider reservation never outlives the attempt that created it — see
     * {@link FluidOrderBook#recordDispatched}.
     */
    private void tickFluidDispatch() {
        FluidOrderBook.FluidDispatchCommand cmd;
        while ((cmd = fluidOrderBook.nextDispatchable()) != null) {
            NetDbg.out("[Network {}] Fluid dispatch: {} | provider={} -> requester={} | {} mB {}",
                    getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8),
                    cmd.provider(), cmd.requester(), cmd.amountMb(), cmd.fluid());
            long shippedMb = 0;
            try {
                shippedMb = worldView.dispatchFluid(
                        cmd.provider(), cmd.requester(), cmd.fluid(), cmd.amountMb(), cmd.orderId());
                if (shippedMb > 0) {
                    NetDbg.out("[Network {}] Fluid shipped: {} | {} mB",
                            getNetworkIdShort(id), cmd.orderId().toString().substring(0, 8), shippedMb);
                }
            } catch (Exception e) {
                LogisticsMod.LOGGER.error("[Network {}] Fluid dispatch failed for order {}: {}",
                        getNetworkIdShort(id), cmd.orderId(), e.getMessage(), e);
            } finally {
                fluidOrderBook.recordDispatched(cmd, shippedMb);
            }
            if (shippedMb <= 0) {
                // Broken/exhausted provider: don't retry every tick, wait for its next scan.
                fluidOrderBook.removeSupply(cmd.provider());
            }
        }
    }

    @Override
    public void registerSink(BlockPos pos, int priority) {
        sinkResolver.registerSink(pos, priority);
    }

    @Override
    public void unregisterSink(BlockPos pos) {
        sinkResolver.unregisterSink(pos);
    }

    @Override
    public void registerSinkInterest(BlockPos pos, Item item) {
        sinkResolver.registerSinkInterest(pos, item);
    }

    @Override
    public void unregisterSinkInterests(BlockPos pos) {
        sinkResolver.unregisterSinkInterests(pos);
    }

    @Override
    public void registerGenericSinkInterest(BlockPos pos) {
        sinkResolver.registerGenericSinkInterest(pos);
    }

    @Override
    public void unregisterGenericSinkInterest(BlockPos pos) {
        sinkResolver.unregisterGenericSinkInterest(pos);
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

        NetDbg.out("[Network {}] Finding sink for {} (members: {}, registered sinks: {})",
                getNetworkIdShort(id), stack.getItem(), graph.size(), sinkResolver.registeredSinkCount());

        BlockPos bestSink = sinkResolver.findSinkFor(stack);

        if (bestSink == null) {
            NetDbg.out("[Network {}] No sink found for {}", getNetworkIdShort(id), stack.getItem());
        } else {
            NetDbg.out("[Network {}] Found sink at {}", getNetworkIdShort(id), bestSink);
        }

        return bestSink;
    }

    @Override
    public BlockPos findFilteredSinkFor(ItemStack stack) {
        if (worldView == null) {
            throw new IllegalStateException("Cannot use findFilteredSinkFor without IWorldView - update tests to use proper constructor");
        }
        return sinkResolver.findFilteredSinkFor(stack);
    }

    @Override
    public void registerSatellite(String id, BlockPos pos) {
        if (id != null && !id.isEmpty() && pos != null) {
            // Only register if unowned or re-registering by the same pipe; ignore collisions
            BlockPos existing = satellites.get(id);
            if (existing == null || existing.equals(pos)) {
                satellites.put(id, pos);
            }
        }
    }

    @Override
    public void unregisterSatellite(String id, BlockPos pos) {
        if (id != null && java.util.Objects.equals(pos, satellites.get(id))) {
            satellites.remove(id);
        }
    }

    @Override
    public BlockPos findSatellite(String id) {
        return id != null ? satellites.get(id) : null;
    }

    @Override
    public java.util.Set<String> getAvailableSatelliteIds() {
        return java.util.Collections.unmodifiableSet(satellites.keySet());
    }

    /** Access the job coordinator for this network (for diagnostics or future API use). */
    public JobCoordinator getJobCoordinator() {
        return jobCoordinator;
    }

    // ===== Energy Operations =====

    @Override
    public void registerEnergySource(BlockPos pos) {
        energySources.add(pos);
    }

    @Override
    public void unregisterEnergySource(BlockPos pos) {
        energySources.remove(pos);
    }

    @Override
    public boolean isPowered() {
        if (worldView == null || energySources.isEmpty()) return false;

        // Reuse this tick's result: all pipes on the network ask the same question each tick.
        long now = worldView.gameTime();
        if (now == lastPoweredCheckTick) return cachedPowered;

        boolean powered = false;
        for (BlockPos pos : energySources) {
            IEnergyStorage storage = worldView.energyStorageAt(pos);
            if (storage != null && storage.getAmount() > 0) {
                powered = true;
                break;
            }
        }
        lastPoweredCheckTick = now;
        cachedPowered = powered;
        return powered;
    }

    @Override
    public boolean consumeEnergy(long amount) {
        if (amount <= 0) return true;
        if (worldView == null || energySources.isEmpty()) return false;

        // Phase 1: build a fixed draw plan by simulating across sources in registration order.
        Map<BlockPos, Long> plannedDraws = new LinkedHashMap<>();
        long planned = 0;
        for (BlockPos pos : energySources) {
            if (planned >= amount) break;
            IEnergyStorage storage = worldView.energyStorageAt(pos);
            if (storage == null) continue;
            long take = storage.extract(amount - planned, true);
            if (take > 0) {
                plannedDraws.put(pos, take);
                planned += take;
            }
        }
        if (planned < amount) return false;

        // Phase 2: commit exactly the planned amount from each recorded source. Within a single
        // synchronous tick nothing else mutates these buffers, so each commit matches its plan.
        long drawn = 0;
        for (Map.Entry<BlockPos, Long> draw : plannedDraws.entrySet()) {
            IEnergyStorage storage = worldView.energyStorageAt(draw.getKey());
            if (storage == null) continue;
            drawn += storage.extract(draw.getValue(), false);
        }
        return drawn >= amount;
    }

    /**
     * Merge another network into this one.
     */
    public void merge(PipeNetwork other) {
        graph.merge(other.graph);
        controller.merge(other.controller);
        fluidOrderBook.merge(other.fluidOrderBook);
        jobCoordinator.merge(other.jobCoordinator);
        sinkResolver.merge(other.sinkResolver);
        energySources.addAll(other.energySources);
        other.satellites.forEach((satId, pos) -> {
            BlockPos existing = satellites.get(satId);
            if (existing != null && !existing.equals(pos)) {
                LogisticsMod.LOGGER.warn(
                        "[Network merge] Satellite ID '{}' collision: {} (this) vs {} (other); overwriting",
                        satId, existing, pos);
            }
            satellites.put(satId, pos);
        });
    }
}
