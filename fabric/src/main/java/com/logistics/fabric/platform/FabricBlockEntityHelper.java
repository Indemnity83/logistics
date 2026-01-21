package com.logistics.fabric.platform;

import com.logistics.platform.services.IBlockEntityHelper;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric implementation of IBlockEntityHelper using FabricBlockEntityTypeBuilder.
 */
public class FabricBlockEntityHelper implements IBlockEntityHelper {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createType(
            BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }
}
