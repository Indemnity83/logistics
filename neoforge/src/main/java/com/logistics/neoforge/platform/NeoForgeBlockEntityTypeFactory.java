package com.logistics.neoforge.platform;

import com.logistics.core.lib.block.BlockEntitySupplier;
import com.logistics.core.lib.block.BlockEntityTypeFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * NeoForge implementation of {@link BlockEntityTypeFactory}.
 *
 * <p>MC 26.1 removed {@code BlockEntityType.Builder}; the type is constructed directly
 * via its constructor (access-widened by NeoForge).
 *
 * <p>An empty block list is allowed, as on Fabric: a type shared by a family of blocks (every farm
 * uses one) can exist before any block of the family does.
 */
public final class NeoForgeBlockEntityTypeFactory implements BlockEntityTypeFactory {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> build(BlockEntitySupplier<T> factory, Block... blocks) {
        if (blocks == null) {
            throw new IllegalArgumentException("build() requires a block array; factory=" + factory.getClass().getName());
        }
        return new BlockEntityType<>(factory::create, Set.of(blocks));
    }
}
