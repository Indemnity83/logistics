package com.logistics.fabric;

import com.logistics.core.lib.BlockEntitySupplier;
import com.logistics.core.lib.BlockEntityTypeFactory;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class FabricBlockEntityBuilder implements BlockEntityTypeFactory {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> build(BlockEntitySupplier<T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::create, blocks).build();
    }
}
