package com.logistics.neoforge.platform;

import com.logistics.core.lib.block.BlockEntitySupplier;
import com.logistics.core.lib.block.BlockEntityTypeFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * NeoForge implementation of {@link BlockEntityTypeFactory}.
 */
public final class NeoForgeBlockEntityTypeFactory implements BlockEntityTypeFactory {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> build(BlockEntitySupplier<T> factory, Block... blocks) {
        if (blocks == null || blocks.length == 0) {
            throw new IllegalArgumentException(
                    "build() requires at least one block; factory=" + factory.getClass().getName());
        }
        return BlockEntityType.Builder.of(factory::create, blocks).build(null);
    }
}
