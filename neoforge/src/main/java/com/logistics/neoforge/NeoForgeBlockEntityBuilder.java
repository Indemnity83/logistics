package com.logistics.neoforge;

import com.logistics.core.lib.BlockEntitySupplier;
import com.logistics.core.lib.BlockEntityTypeFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NeoForgeBlockEntityBuilder implements BlockEntityTypeFactory {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> build(BlockEntitySupplier<T> factory, Block... blocks) {
        return BlockEntityType.Builder.of(factory::create, blocks).build();
    }
}
