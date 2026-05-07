package com.logistics.core.lib.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ServiceLoader;

public interface BlockEntityTypeFactory {
    <T extends BlockEntity> BlockEntityType<T> build(BlockEntitySupplier<T> factory, Block... blocks);

    BlockEntityTypeFactory INSTANCE = ServiceLoader.load(BlockEntityTypeFactory.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No BlockEntityTypeFactory service found"));
}
