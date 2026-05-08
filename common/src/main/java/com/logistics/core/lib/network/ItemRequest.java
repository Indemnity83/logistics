package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

/**
 * A request to the logistics network to source and deliver items to a destination.
 * Submitted to the job coordinator to create a network job.
 * Pure value object — no Minecraft world coupling beyond BlockPos/IItemKey.
 */
public record ItemRequest(
        IItemKey item,
        long amount,
        BlockPos destination,
        FulfillmentMode fulfillmentMode) {

    public ItemRequest {
        if (item == null) throw new NullPointerException("item must not be null");
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (fulfillmentMode == null) throw new NullPointerException("fulfillmentMode must not be null");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive, got: " + amount);
    }

    /** Convenience factory for the common PARTIAL fulfillment case. */
    public static ItemRequest partial(IItemKey item, long amount, BlockPos destination) {
        return new ItemRequest(item, amount, destination, FulfillmentMode.PARTIAL);
    }
}
