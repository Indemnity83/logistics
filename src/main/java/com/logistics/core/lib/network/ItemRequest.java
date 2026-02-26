package com.logistics.core.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a request for items from a Requester pipe.
 * Requests are queued in PipeNetwork and matched to available providers.
 */
public record ItemRequest(
    BlockPos requester,
    ItemStack stack,
    long amount,
    long createdAt,
    int priority
) {
    public ItemRequest(BlockPos requester, ItemStack stack, long amount, long createdAt) {
        this(requester, stack, amount, createdAt, 0);
    }
}
