package com.logistics.api;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface TransportApi {
    boolean isTransportBlock(BlockState state);

    /**
     * @return true if accepted; false to let callers fall back to inventory/drop handling.
     */
    boolean tryInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from);

    /**
     * Force insertion, bypassing ingress checks where possible.
     * @return true if accepted; false to let callers fall back to inventory/drop handling.
     */
    boolean forceInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from);
}
