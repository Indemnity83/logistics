package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class QuartzPipeBlock extends PipeBlock {
    public QuartzPipeBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity)) {
            return 0;
        }
        return pipeEntity.getComparatorOutput();
    }
}
