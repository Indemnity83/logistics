package com.logistics.forestry.humus;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.forestry.logic.LevelTreeView;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rich soil that gives the sapling standing on it extra growth steps. A tree standing on it spends
 * it: Humus with wood above turns to sand, whether its own boost grew the tree or not.
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
        if (turnToSandUnderTree(level, pos)) {
            return;
        }
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
        turnToSandUnderTree(level, pos);
    }

    /** Turns the Humus at {@code pos} to sand when a tree's wood stands on it; true when it did. */
    private boolean turnToSandUnderTree(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this) || !level.getBlockState(pos.above()).is(LevelTreeView.FELLABLE_WOOD)) {
            return false;
        }
        level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
        return true;
    }
}
