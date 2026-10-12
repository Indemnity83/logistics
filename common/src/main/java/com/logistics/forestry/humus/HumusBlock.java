package com.logistics.forestry.humus;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rich soil that gives the sapling standing on it extra growth steps. Each tree it grows may wear
 * it back down to dirt; nothing else does.
 */
public class HumusBlock extends Block {

    public HumusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos saplingPos = pos.above();
        BlockState above = level.getBlockState(saplingPos);
        if (!(above.getBlock() instanceof SaplingBlock sapling)) {
            return;
        }
        if (!HumusGrowth.hasLightToGrow(level.getMaxLocalRawBrightness(saplingPos.above()))) {
            return;
        }
        if (!HumusGrowth.shouldBoost(random, LogisticsConfigHost.get(LogisticsForestry.CONFIG.HUMUS_GROWTH_CHANCE))) {
            return;
        }

        sapling.advanceTree(level, saplingPos, above, random);

        boolean treeGrew = !(level.getBlockState(saplingPos).getBlock() instanceof SaplingBlock);
        if (HumusGrowth.shouldDegrade(treeGrew, random, LogisticsConfigHost.get(LogisticsForestry.CONFIG.HUMUS_DEGRADE_CHANCE))
                && level.getBlockState(pos).is(this)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }
}
