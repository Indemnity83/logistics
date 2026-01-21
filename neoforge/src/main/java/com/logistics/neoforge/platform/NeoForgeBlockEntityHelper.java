package com.logistics.neoforge.platform;

import com.logistics.platform.services.IBlockEntityHelper;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge implementation of IBlockEntityHelper using BlockEntityType.Builder.
 */
public class NeoForgeBlockEntityHelper implements IBlockEntityHelper {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends net.minecraft.block.entity.BlockEntity> net.minecraft.block.entity.BlockEntityType<T> createType(
            BiFunction<net.minecraft.util.math.BlockPos, net.minecraft.block.BlockState, T> factory,
            net.minecraft.block.Block... blocks) {
        // Convert Yarn-mapped types to Mojmap
        // This will need to be addressed with proper mapping configuration
        Block[] neoBlocks = new Block[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            neoBlocks[i] = (Block) (Object) blocks[i];
        }

        BlockEntityType<BlockEntity> type = BlockEntityType.Builder.of(
                        (pos, state) -> {
                            // Convert Mojmap types back to Yarn for the factory
                            net.minecraft.util.math.BlockPos yarnPos =
                                    new net.minecraft.util.math.BlockPos(pos.getX(), pos.getY(), pos.getZ());
                            return (BlockEntity) (Object) factory.apply(yarnPos, (net.minecraft.block.BlockState) (Object) state);
                        },
                        neoBlocks)
                .build(null);

        return (net.minecraft.block.entity.BlockEntityType<T>) (Object) type;
    }
}
