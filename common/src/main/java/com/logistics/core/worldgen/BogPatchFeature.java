package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Turns soil into bog earth in organic patches: seeds on the surface block at the origin (whatever the
 * config's {@code target} matches — mud in mangrove swamps, grass/dirt at regular-swamp water edges),
 * then flood-fills (6-neighbour, 3D) through adjacent matching blocks, each step a {@code spread_chance}
 * roll, capped at {@code max_count}. Only {@code target} blocks are ever replaced.
 */
public class BogPatchFeature extends Feature<BogPatchConfiguration> {

    public BogPatchFeature(Codec<BogPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BogPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BogPatchConfiguration config = context.config();
        BlockPredicate target = config.target();

        // The heightmap origin sits just above the surface; the soil is the block below it.
        BlockPos origin = context.origin();
        BlockPos below = origin.below();
        BlockPos seed = target.test(level, below) ? below : (target.test(level, origin) ? origin : null);
        if (seed == null) {
            return false;
        }

        BlockState bogEarth = LogisticsCore.BLOCK.BOG_EARTH.defaultBlockState();
        Set<Long> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.asLong());

        int placed = 0;
        while (!queue.isEmpty() && placed < config.maxCount()) {
            BlockPos pos = queue.poll();
            if (!target.test(level, pos)) {
                continue;
            }
            level.setBlock(pos, bogEarth, 2);
            placed++;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next.asLong())
                        && target.test(level, next)
                        && random.nextFloat() < config.spreadChance()) {
                    queue.add(next.immutable());
                }
            }
        }
        return placed > 0;
    }
}
