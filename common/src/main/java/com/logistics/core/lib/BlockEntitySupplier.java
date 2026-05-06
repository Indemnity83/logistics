package com.logistics.core.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockEntitySupplier<T extends BlockEntity> {
    T create(BlockPos pos, BlockState state);
}
