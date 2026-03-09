package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import java.util.UUID;

/**
 * A standing order for items. Placed by requesters (supplier/requester pipes or crafting modules).
 * Persists in the network until explicitly cancelled or fulfilled via notifyDelivery.
 * Replaces ItemRequest + LogisticsOrder.
 */
public record Order(UUID id, ItemVariant item, long amount, BlockPos requester) {
    public Order {
        if (id == null) throw new NullPointerException("id must not be null");
        if (item == null) throw new NullPointerException("item must not be null");
        if (requester == null) throw new NullPointerException("requester must not be null");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive, got: " + amount);
    }
}
