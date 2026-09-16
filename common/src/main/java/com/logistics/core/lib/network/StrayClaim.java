package com.logistics.core.lib.network;

import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * A standing order a stray packet has been re-homed onto: the requester it should now travel to,
 * and the delivery id the arrival must acknowledge.
 *
 * <p>Returned by {@link ILogisticsNetwork#claimStrayDelivery}. The claim is already recorded in the
 * network's books when this is handed back — the amount has moved from queued to in-transit — so the
 * caller must put both fields on the traveling item, or the acknowledgement will never arrive and the
 * requester's outstanding total will sit high until the order is cancelled.
 *
 * @param destination requester pipe the packet is now addressed to
 * @param deliveryId  id of the claimed order, to be carried by the packet
 */
public record StrayClaim(BlockPos destination, UUID deliveryId) {
    public StrayClaim {
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (deliveryId == null) throw new NullPointerException("deliveryId must not be null");
    }
}
