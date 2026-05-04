package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * A single item reservation: a commitment that a specific provider holds {@code amount} units
 * of {@code item} for a specific order. State transitions are managed by {@link ReservationManager}.
 *
 * <p>Mutable (state field changes over the reservation lifecycle). Not a record.
 */
public final class ItemReservation {
    /** Unique identity of this reservation. */
    public final ReservationId id;

    /** The order this reservation was created for. */
    public final UUID orderId;

    /** Provider pipe holding the stock. */
    public final BlockPos provider;

    /** Requester pipe that placed the order. */
    public final BlockPos requester;

    /** Item being reserved. */
    public final ItemVariant item;

    /** Amount reserved. */
    public final long amount;

    /**
     * Whether this is a hard reservation (committed, cannot be bumped) vs.
     * soft (lower-priority, can be displaced by a higher-priority order in a future phase).
     */
    public final boolean hard;

    /** Current lifecycle state. Mutated by {@link ReservationManager}. */
    AllocationState state;

    ItemReservation(ReservationId id, UUID orderId, BlockPos provider, BlockPos requester,
                    ItemVariant item, long amount, boolean hard, AllocationState initialState) {
        this.id = id;
        this.orderId = orderId;
        this.provider = provider;
        this.requester = requester;
        this.item = item;
        this.amount = amount;
        this.hard = hard;
        this.state = initialState;
    }

    public AllocationState state() {
        return state;
    }

    @Override
    public String toString() {
        return id + " order=" + orderId.toString().substring(0, 8)
                + " " + amount + "x " + item.toStack().getItem()
                + " from=" + provider + " to=" + requester
                + " [" + state + (hard ? ",hard" : ",soft") + "]";
    }
}
