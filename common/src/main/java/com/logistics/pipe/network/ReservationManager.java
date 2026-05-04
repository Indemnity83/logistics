package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks soft and hard reservations against provider supply.
 *
 * <p>Replaces the implicit {@code SupplyEntry.available} decrement pattern with an explicit
 * reservation lifecycle: {@code RESERVED → IN_TRANSIT → DELIVERED} (or {@code INVALIDATED}).
 *
 * <p>Effective availability for a provider/item pair is:
 * <pre>  raw - sum(amount of active reservations for (provider, item))</pre>
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class ReservationManager {

    // Primary index: reservationId → reservation
    private final Map<ReservationId, ItemReservation> byId = new HashMap<>();

    // ===== Reservation Lifecycle =====

    /**
     * Create a new reservation against a specific provider.
     *
     * @param orderId   the order this reservation backs
     * @param provider  provider pipe holding the stock
     * @param requester requester pipe that placed the order
     * @param item      item being reserved
     * @param amount    amount to reserve
     * @param hard      true = committed; false = can be bumped by higher-priority orders
     * @return a new {@link ReservationId} for this reservation
     */
    public ReservationId reserve(UUID orderId, BlockPos provider, BlockPos requester,
                                 ItemVariant item, long amount, boolean hard) {
        ReservationId id = ReservationId.random();
        byId.put(id, new ItemReservation(id, orderId, provider, requester, item, amount, hard,
                AllocationState.RESERVED));
        return id;
    }

    /**
     * Transition a reservation to a new state by its ID.
     * No-op if the reservation does not exist.
     */
    public void transition(ReservationId id, AllocationState newState) {
        ItemReservation res = byId.get(id);
        if (res != null) res.state = newState;
    }

    /**
     * Transition the reservation associated with an order to a new state.
     * Affects the first active reservation found for the given order.
     */
    public void transitionByOrder(UUID orderId, AllocationState newState) {
        for (ItemReservation res : byId.values()) {
            if (res.orderId.equals(orderId) && res.state.isActive()) {
                res.state = newState;
                return;
            }
        }
    }

    /**
     * Release (remove) a reservation entirely.
     * Use for cancellation — the reservation leaves no trace.
     */
    public void release(ReservationId id) {
        byId.remove(id);
    }

    /**
     * Release all reservations associated with an order (e.g. order cancelled).
     */
    public void releaseByOrder(UUID orderId) {
        byId.values().removeIf(r -> r.orderId.equals(orderId));
    }

    /**
     * Mark a reservation as {@link AllocationState#INVALIDATED}.
     * Invalidated reservations no longer count against effective availability but remain
     * visible for replanning (future phase).
     */
    public void invalidate(ReservationId id) {
        ItemReservation res = byId.get(id);
        if (res != null) res.state = AllocationState.INVALIDATED;
    }

    /**
     * Invalidate all active reservations for a provider that became unavailable.
     * Called when a provider returned 0 items during dispatch.
     */
    public void invalidateByProvider(BlockPos provider) {
        for (ItemReservation res : byId.values()) {
            if (res.provider.equals(provider) && res.state.isActive()) {
                res.state = AllocationState.INVALIDATED;
            }
        }
    }

    /**
     * Mark the first {@link AllocationState#IN_TRANSIT} reservation matching
     * {@code (requester, item)} as {@link AllocationState#DELIVERED} and release it.
     * Called when a {@code TravelingItem} physically arrives at its destination.
     */
    public void markDelivered(BlockPos requester, ItemVariant item) {
        for (var it = byId.entrySet().iterator(); it.hasNext(); ) {
            ItemReservation res = it.next().getValue();
            if (res.requester.equals(requester) && res.item.equals(item)
                    && res.state == AllocationState.IN_TRANSIT) {
                it.remove(); // DELIVERED → release immediately, no replanning needed
                return;
            }
        }
    }

    // ===== Availability Query =====

    /**
     * Compute effective available stock for a provider/item pair.
     * Deducts the sum of all active reservations from the raw registered amount.
     *
     * @param provider provider pipe position
     * @param item     item variant to query
     * @param raw      raw registered supply amount (from {@code SupplyEntry.available})
     * @return max(0, raw − sum of active reservations)
     */
    public long effectiveAvailable(BlockPos provider, ItemVariant item, long raw) {
        long reserved = 0;
        for (ItemReservation res : byId.values()) {
            if (res.provider.equals(provider) && res.item.equals(item) && res.state.isActive()) {
                reserved += res.amount;
            }
        }
        return Math.max(0L, raw - reserved);
    }

    // ===== Query =====

    /**
     * Return all reservations (in any state) for a given provider, for diagnostics.
     */
    public List<ItemReservation> getReservations(BlockPos provider) {
        List<ItemReservation> result = new ArrayList<>();
        for (ItemReservation res : byId.values()) {
            if (res.provider.equals(provider)) result.add(res);
        }
        return result;
    }

    // ===== Merge =====

    /**
     * Absorb all reservations from another manager (used when two networks join).
     */
    public void merge(ReservationManager other) {
        byId.putAll(other.byId);
    }
}
