package com.logistics.platform.services;

import java.util.function.BiFunction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Platform-agnostic service for creating block entity types.
 * Abstracts FabricBlockEntityTypeBuilder and BlockEntityType.Builder.
 */
public interface IBlockEntityHelper {

    /**
     * Create a BlockEntityType with the given factory and valid blocks.
     *
     * @param factory function to create the block entity
     * @param blocks the blocks this entity type is valid for
     * @param <T> the block entity type
     * @return the created BlockEntityType
     */
    <T extends BlockEntity> BlockEntityType<T> createType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks);
}
