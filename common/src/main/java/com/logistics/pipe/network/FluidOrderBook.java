package com.logistics.pipe.network;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.network.FluidResourceKey;
import com.logistics.core.lib.network.FulfillmentMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluid-native supply/order bookkeeping, parallel to (never touching) {@link NetworkController}'s
 * item-keyed machinery.
 *
 * <p>Fluids have no crafting concept — {@code FluidProviderModule} only ever advertises real tank
 * stock — so this is deliberately a trimmed analogue of {@link NetworkController}'s real-stock dispatch
 * path: a supply table, a FIFO order queue, an outstanding-mB-per-requester map, and a dispatch loop.
 * It omits {@code ReservationManager}'s soft/hard reservation lifecycle, {@code inTransitOrders}
 * automatic requeue-on-failure, and the recursive {@code ProviderCanFulfill} ingredient-chain
 * machinery, none of which fluids need.
 *
 * <p>Network identity for bookkeeping is {@link FluidResourceKey} (the {@link Fluid} alone) — never a
 * physical packet. A physical packet's real, positive {@code amountMb} is carried purely for transport
 * and destination insertion; see {@code FluidPacket}.
 *
 * <p>Zero Minecraft-world coupling beyond {@link BlockPos}/{@link Fluid} — 100% testable with pure Java.
 */
public class FluidOrderBook {

    /** Raw advertised supply for one provider, as last reported by its periodic tank scan. */
    private record FluidSupplyEntry(BlockPos pos, long rawAvailableMb, int priority) {}

    /**
     * A standing (or in-transit) fluid order. Mirrors {@link com.logistics.core.lib.network.Order}'s
     * validation shape.
     */
    private record FluidOrder(
            UUID id, FluidResourceKey fluid, long amountMb, BlockPos requester, FulfillmentMode fulfillmentMode) {
        FluidOrder {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(fluid, "fluid");
            Objects.requireNonNull(requester, "requester");
            Objects.requireNonNull(fulfillmentMode, "fulfillmentMode");
            if (amountMb <= 0) throw new IllegalArgumentException("amountMb must be positive, got: " + amountMb);
        }
    }

    /** Command returned by {@link #nextDispatchable()} describing what the network wants dispatched. */
    public record FluidDispatchCommand(UUID orderId, BlockPos provider, BlockPos requester, Fluid fluid, long amountMb) {}

    // Per-fluid supply entries, sorted by priority ascending (mirrors NetworkController.supplyTable)
    private final Map<FluidResourceKey, List<FluidSupplyEntry>> supplyTable = new HashMap<>();

    // Capacity committed to a dispatch attempt but not yet confirmed by a fresh supply scan. Only the
    // UNSHIPPED portion of a reservation is ever released early (see recordDispatched) — the SHIPPED
    // portion deliberately stays committed until registerSupply/removeSupply next runs for that
    // provider, since that's what stops a second dispatch (in the same tick, before rescan) from
    // allocating fluid that's already been physically extracted.
    private final Map<BlockPos, Long> committedMbByProvider = new HashMap<>();

    // Standing orders not yet dispatched, FIFO
    private final Map<UUID, FluidOrder> orderQueue = new LinkedHashMap<>();

    // Dispatched, physically in transit (not yet delivered or failed)
    private final Map<UUID, FluidOrder> inTransitOrders = new HashMap<>();

    // Per-requester outstanding mB = queued + in-transit. Decremented ONLY by a validated
    // delivery/failure acknowledgement. This is what getOrderedAmountFor returns.
    private final Map<BlockPos, Map<FluidResourceKey, Long>> orderedForRequester = new HashMap<>();

    // ===== Supply Registration =====

    /**
     * Register (or replace) a provider's advertised fluid supply. Clears any stale commitment for this
     * provider (a fresh scan supersedes it — see {@link #committedMbByProvider}'s javadoc).
     *
     * @param pos        provider position
     * @param fluid      the fluid advertised (never {@code null} — call {@link #removeSupply} instead
     *                   of registering an empty/absent fluid)
     * @param availableMb raw mB currently in the provider's tank
     * @param priority   dispatch priority (lower = preferred)
     */
    public void registerSupply(BlockPos pos, Fluid fluid, long availableMb, int priority) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(fluid, "fluid");

        removeFromSupplyTable(pos);
        committedMbByProvider.remove(pos);

        if (availableMb <= 0) return;

        FluidResourceKey key = new FluidResourceKey(fluid);
        List<FluidSupplyEntry> list = supplyTable.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(new FluidSupplyEntry(pos, availableMb, priority));
        list.sort(Comparator.comparingInt(FluidSupplyEntry::priority));
    }

    /** Remove all supply for a provider position (no tank found, tank emptied, or pipe removed). */
    public void removeSupply(BlockPos pos) {
        removeFromSupplyTable(pos);
        committedMbByProvider.remove(pos);
    }

    private void removeFromSupplyTable(BlockPos pos) {
        for (List<FluidSupplyEntry> entries : supplyTable.values()) {
            entries.removeIf(e -> e.pos().equals(pos));
        }
        supplyTable.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    // ===== Order Management =====

    /** Place a standing order. Increments {@code orderedForRequester} immediately. */
    public UUID placeOrder(Fluid fluid, long amountMb, BlockPos requester, FulfillmentMode fulfillmentMode) {
        return placeOrder(UUID.randomUUID(), fluid, amountMb, requester, fulfillmentMode);
    }

    /** Place a standing order with default {@link FulfillmentMode#PARTIAL} fulfillment. */
    public UUID placeOrder(Fluid fluid, long amountMb, BlockPos requester) {
        return placeOrder(fluid, amountMb, requester, FulfillmentMode.PARTIAL);
    }

    /**
     * Package-private overload accepting an explicit order id — used by tests that need to construct a
     * specific UUID (e.g. simulating a network split-then-rejoin where two independently-maintained
     * books share the same order id, for {@link #merge} collision coverage). Production callers always
     * go through the random-id overload above.
     */
    UUID placeOrder(UUID id, Fluid fluid, long amountMb, BlockPos requester, FulfillmentMode fulfillmentMode) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(fulfillmentMode, "fulfillmentMode");
        if (amountMb <= 0) throw new IllegalArgumentException("amountMb must be positive, got: " + amountMb);

        FluidResourceKey key = new FluidResourceKey(fluid);
        orderQueue.put(id, new FluidOrder(id, key, amountMb, requester, fulfillmentMode));
        orderedForRequester.computeIfAbsent(requester, k -> new HashMap<>()).merge(key, amountMb, Long::sum);
        NetDbg.out("Fluid order placed: {} | {} mB {} -> {} (requester)",
                id.toString().substring(0, 8), amountMb, fluid, requester);
        return id;
    }

    /**
     * Cancel a standing or in-transit order by ID. Decrements {@code orderedForRequester}. A queued
     * (not yet dispatched) order has no provider reservation to release, since reservations are only
     * created at dispatch time.
     */
    public void cancelOrder(UUID id) {
        FluidOrder order = orderQueue.remove(id);
        if (order != null) {
            decrementOrdered(order.requester(), order.fluid(), order.amountMb());
            NetDbg.out("Fluid order cancelled: {}", id.toString().substring(0, 8));
            return;
        }
        FluidOrder inTransit = inTransitOrders.remove(id);
        if (inTransit != null) {
            decrementOrdered(inTransit.requester(), inTransit.fluid(), inTransit.amountMb());
            NetDbg.out("In-transit fluid order cancelled: {}", id.toString().substring(0, 8));
        }
    }

    /** Cancel all standing and in-transit orders for a requester (e.g. pipe removal). */
    public void cancelOrdersFor(BlockPos requester) {
        orderQueue.entrySet().removeIf(e -> e.getValue().requester().equals(requester));
        inTransitOrders.entrySet().removeIf(e -> e.getValue().requester().equals(requester));
        orderedForRequester.remove(requester);
    }

    // ===== Dispatch =====

    /**
     * Find the next dispatchable order and reserve its capacity against the chosen provider.
     *
     * @return a dispatch command, or {@code null} if no order can be fulfilled right now
     */
    public FluidDispatchCommand nextDispatchable() {
        for (FluidOrder order : orderQueue.values()) {
            List<FluidSupplyEntry> entries = supplyTable.get(order.fluid());
            if (entries == null || entries.isEmpty()) continue;
            FluidDispatchCommand command = tryDispatch(order, entries);
            if (command != null) return command;
        }
        return null;
    }

    /**
     * Attempt to create a dispatch command for one order against its supply entries, tried in priority
     * order. A provider with no {@link #effectiveAvailable} right now is skipped in favor of the next.
     */
    private FluidDispatchCommand tryDispatch(FluidOrder order, List<FluidSupplyEntry> entries) {
        for (FluidSupplyEntry supply : entries) {
            long effective = effectiveAvailable(supply.pos(), order.fluid().fluid());
            if (effective <= 0) continue;

            if (effective >= order.amountMb()) {
                reserve(supply.pos(), order.amountMb());
                return new FluidDispatchCommand(
                        order.id(), supply.pos(), order.requester(), order.fluid().fluid(), order.amountMb());
            }

            // Partial fill: FULL mode blocks dispatch unless total network-wide stock covers the order.
            if (order.fulfillmentMode() == FulfillmentMode.FULL
                    && getAvailableMbFor(order.fluid().fluid()) < order.amountMb()) {
                return null;
            }

            reserve(supply.pos(), effective);
            return new FluidDispatchCommand(order.id(), supply.pos(), order.requester(), order.fluid().fluid(), effective);
        }
        return null;
    }

    private void reserve(BlockPos provider, long amountMb) {
        committedMbByProvider.merge(provider, amountMb, Long::sum);
    }

    /**
     * Effective dispatchable capacity for a provider: raw advertised supply minus whatever is still
     * committed to earlier, not-yet-rescanned dispatch attempts.
     */
    public long effectiveAvailable(BlockPos provider, Fluid fluid) {
        List<FluidSupplyEntry> entries = supplyTable.get(new FluidResourceKey(fluid));
        if (entries == null) return 0L;
        for (FluidSupplyEntry e : entries) {
            if (e.pos().equals(provider)) {
                long committed = committedMbByProvider.getOrDefault(provider, 0L);
                return Math.max(0L, e.rawAvailableMb() - committed);
            }
        }
        return 0L;
    }

    /**
     * Total raw (not reservation-adjusted) advertised supply for a fluid across all providers — used
     * only for {@link FulfillmentMode#FULL} gating, mirroring {@code NetworkController.getAvailableAmount}'s
     * raw-sum semantics exactly (that check also does not deduct active reservations).
     */
    public long getAvailableMbFor(Fluid fluid) {
        List<FluidSupplyEntry> entries = supplyTable.get(new FluidResourceKey(fluid));
        if (entries == null || entries.isEmpty()) return 0L;
        long total = 0;
        for (FluidSupplyEntry e : entries) total += e.rawAvailableMb();
        return total;
    }

    /**
     * Record the outcome of a dispatch attempt. Always reconciles the reservation created by
     * {@link #nextDispatchable()}: the <b>unshipped</b> portion ({@code command.amountMb() -
     * actualShippedMb}) is released immediately; the <b>shipped</b> portion deliberately stays
     * committed (see {@link #committedMbByProvider}) until the provider's next scan. Call this exactly
     * once per dispatch attempt, including from a catch block if the dispatch call throws (with
     * {@code actualShippedMb = 0}) — a reservation must never outlive the attempt it was created for by
     * more than the shortfall.
     *
     * <p>Does not touch {@code orderedForRequester} — shipped mB is "in flight," not "delivered."
     *
     * @param command        the command returned by {@link #nextDispatchable()}
     * @param actualShippedMb actual mB shipped (0 if the provider had nothing usable or the call threw)
     */
    public void recordDispatched(FluidDispatchCommand command, long actualShippedMb) {
        long shipped = Math.max(0L, actualShippedMb);
        if (shipped > command.amountMb()) {
            LogisticsMod.LOGGER.warn(
                    "Fluid dispatch {} reported {} mB shipped but only {} mB was reserved -- clamping",
                    command.orderId(), shipped, command.amountMb());
            shipped = command.amountMb();
        }

        long unshipped = command.amountMb() - shipped;
        if (unshipped > 0) {
            Long remaining = committedMbByProvider.merge(command.provider(), -unshipped, Long::sum);
            if (remaining != null && remaining <= 0) committedMbByProvider.remove(command.provider());
        }

        if (shipped <= 0) return;

        FluidOrder order = orderQueue.get(command.orderId());
        if (order == null) return; // order was cancelled or already fully dispatched concurrently

        NetDbg.out("Fluid dispatch recorded: {} | {} mB shipped", command.orderId().toString().substring(0, 8), shipped);

        if (shipped >= order.amountMb()) {
            orderQueue.remove(command.orderId());
        } else {
            orderQueue.put(command.orderId(), new FluidOrder(
                    command.orderId(), order.fluid(), order.amountMb() - shipped, order.requester(), order.fulfillmentMode()));
        }

        trackInTransit(command.orderId(), order, shipped);
    }

    private void trackInTransit(UUID orderId, FluidOrder order, long shippedMb) {
        inTransitOrders.merge(
                orderId,
                new FluidOrder(orderId, order.fluid(), shippedMb, order.requester(), order.fulfillmentMode()),
                (existing, added) -> new FluidOrder(
                        existing.id(), existing.fluid(), existing.amountMb() + added.amountMb(),
                        existing.requester(), existing.fulfillmentMode()));
    }

    // ===== Delivery / Failure Acknowledgement =====

    /**
     * Record physical delivery of a tracked dispatch. Validated against the in-transit record before
     * mutating any totals — see {@link #resolveInTransit}.
     */
    public void notifyDelivery(UUID deliveryId, BlockPos requester, Fluid fluid, long amountMb) {
        resolveInTransit(deliveryId, requester, fluid, amountMb, true);
    }

    /**
     * Record failed delivery of a tracked dispatch. Same validation as {@link #notifyDelivery}, but
     * does not automatically requeue — the supplier's own periodic recheck notices the still-reduced
     * outstanding total and re-orders on its own.
     */
    public void notifyDeliveryFailed(UUID deliveryId, BlockPos requester, Fluid fluid, long amountMb) {
        resolveInTransit(deliveryId, requester, fluid, amountMb, false);
    }

    /**
     * Shared validated release-then-decrement for both delivery and failure acknowledgement. Rejects
     * (logs, no mutation) rather than corrupting totals when: the delivery id is unknown or already
     * fully resolved (duplicate acknowledgement), the requester/fluid don't match the tracked order, or
     * the reported amount is non-positive or exceeds what's actually tracked in flight.
     */
    private void resolveInTransit(UUID deliveryId, BlockPos requester, Fluid fluid, long amountMb, boolean delivered) {
        FluidOrder tracked = inTransitOrders.get(deliveryId);
        if (tracked == null) {
            LogisticsMod.LOGGER.warn(
                    "Rejected fluid {} acknowledgement for unknown/already-resolved delivery {} -- ignoring",
                    delivered ? "delivery" : "failure", deliveryId);
            return;
        }

        FluidResourceKey key = new FluidResourceKey(fluid);
        if (!tracked.requester().equals(requester) || !tracked.fluid().equals(key)
                || amountMb <= 0 || amountMb > tracked.amountMb()) {
            LogisticsMod.LOGGER.warn(
                    "Rejected invalid fluid {} acknowledgement for delivery {} (requester={}, fluid={}, amountMb={}) "
                            + "against tracked {}", delivered ? "delivery" : "failure", deliveryId, requester, fluid,
                    amountMb, tracked);
            return;
        }

        if (amountMb >= tracked.amountMb()) {
            inTransitOrders.remove(deliveryId);
        } else {
            inTransitOrders.put(deliveryId, new FluidOrder(
                    tracked.id(), tracked.fluid(), tracked.amountMb() - amountMb, tracked.requester(), tracked.fulfillmentMode()));
        }

        decrementOrdered(requester, key, amountMb);
        NetDbg.out("Fluid {} notified: {} received {} mB of {}",
                delivered ? "delivery" : "failure", requester, amountMb, fluid);
    }

    /**
     * Record failed delivery when no delivery id is available (legacy/untracked drop path). Clamps the
     * decrement to what's actually outstanding rather than letting the map go negative or reduce
     * unrelated demand.
     */
    public void notifyDeliveryFailedNoId(BlockPos requester, Fluid fluid, long amountMb) {
        if (amountMb <= 0) return;
        FluidResourceKey key = new FluidResourceKey(fluid);
        Map<FluidResourceKey, Long> map = orderedForRequester.get(requester);
        long outstanding = map == null ? 0L : map.getOrDefault(key, 0L);
        long decrement = Math.min(amountMb, outstanding);
        if (decrement < amountMb) {
            LogisticsMod.LOGGER.warn(
                    "Untracked fluid delivery-failure for {} reported {} mB but only {} mB outstanding -- clamping",
                    requester, amountMb, outstanding);
        }
        if (decrement > 0) decrementOrdered(requester, key, decrement);
    }

    private void decrementOrdered(BlockPos requester, FluidResourceKey fluid, long amountMb) {
        Map<FluidResourceKey, Long> map = orderedForRequester.get(requester);
        if (map == null) return;
        long remaining = map.merge(fluid, -amountMb, Long::sum);
        if (remaining <= 0) map.remove(fluid);
        if (map.isEmpty()) orderedForRequester.remove(requester);
    }

    // ===== Query =====

    /**
     * Total mB currently ordered for a requester (queued + in-transit) but not yet delivered/failed.
     * Used by the fluid supplier to avoid placing duplicate orders while packets are still traveling.
     */
    public long getOrderedAmountFor(BlockPos requester, Fluid fluid) {
        Map<FluidResourceKey, Long> map = orderedForRequester.get(requester);
        return map == null ? 0L : map.getOrDefault(new FluidResourceKey(fluid), 0L);
    }

    // ===== Merge =====

    /**
     * Merge another order book into this one (used when two networks join). Does not blindly sum
     * {@code orderedForRequester} — an overlapping order UUID between the two books (e.g. from a
     * network split-then-rejoin) would otherwise double-count that order's demand even though the
     * queue/in-transit maps correctly deduplicate it. Instead, {@code orderedForRequester} is rebuilt
     * from scratch from the merged, deduplicated order records.
     */
    public void merge(FluidOrderBook other) {
        for (Map.Entry<FluidResourceKey, List<FluidSupplyEntry>> entry : other.supplyTable.entrySet()) {
            List<FluidSupplyEntry> myList = supplyTable.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            myList.addAll(entry.getValue());
            myList.sort(Comparator.comparingInt(FluidSupplyEntry::priority));
        }

        for (Map.Entry<BlockPos, Long> entry : other.committedMbByProvider.entrySet()) {
            committedMbByProvider.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        for (Map.Entry<UUID, FluidOrder> entry : other.orderQueue.entrySet()) {
            orderQueue.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, FluidOrder> entry : other.inTransitOrders.entrySet()) {
            inTransitOrders.putIfAbsent(entry.getKey(), entry.getValue());
        }

        orderedForRequester.clear();
        for (FluidOrder order : orderQueue.values()) {
            orderedForRequester.computeIfAbsent(order.requester(), k -> new HashMap<>())
                    .merge(order.fluid(), order.amountMb(), Long::sum);
        }
        for (FluidOrder order : inTransitOrders.values()) {
            orderedForRequester.computeIfAbsent(order.requester(), k -> new HashMap<>())
                    .merge(order.fluid(), order.amountMb(), Long::sum);
        }
    }
}
