package com.logistics.pipe.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * A single item reservation: a commitment that a specific provider holds {@code amount} units
 * of {@code item} for a specific order. State transitions are managed by {@link ReservationManager}.
 *
 * <p>Mutable: state changes over the lifecycle, and amount shrinks as parts of the shipment
 * are acknowledged. Not a record.
 */
public final class ItemReservation {
    public final ReservationId id;
    public final UUID orderId;
    public final BlockPos provider;
    public final BlockPos requester;
    public final IItemKey item;
    public long amount;
    public final boolean hard;
    AllocationState state;

    ItemReservation(ReservationId id, UUID orderId, BlockPos provider, BlockPos requester,
                    IItemKey item, long amount, boolean hard, AllocationState initialState) {
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
                + " " + amount + "x " + item.toStack(1).getItem()
                + " from=" + provider + " to=" + requester
                + " [" + state + (hard ? ",hard" : ",soft") + "]";
    }
}
