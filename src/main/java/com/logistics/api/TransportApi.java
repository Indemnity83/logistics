package com.logistics.api;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface TransportApi {
    boolean isTransportBlock(BlockState state);

    /**
     * @return true if accepted; false to let callers fall back to inventory/drop handling.
     */
    boolean tryInsert(ServerWorld world, BlockPos targetPos, ItemStack stack, Direction from);

    /**
     * Force insertion, bypassing ingress checks where possible.
     * @return true if accepted; false to let callers fall back to inventory/drop handling.
     */
    boolean forceInsert(ServerWorld world, BlockPos targetPos, ItemStack stack, Direction from);
}
