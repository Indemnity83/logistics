package com.logistics.pipe.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Represents an order for a Provider pipe to fulfill.
 * Created when a request is matched to an available provider.
 */
public record LogisticsOrder(
    BlockPos provider,
    BlockPos requester,
    ItemStack stack,
    long amount,
    long createdAt
) {}
