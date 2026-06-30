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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Turns mud into bog earth in organic patches: seeds on the exposed surface mud at the origin, then
 * flood-fills (6-neighbour, 3D) through adjacent mud, each step a {@code spread_chance} roll, capped at
 * {@code max_count}. Only mud is ever replaced, so patches hug the muddy surface and never touch other
 * terrain.
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

        // The heightmap origin sits just above the surface; the mud is the block below it.
        BlockPos origin = context.origin();
        BlockPos seed = level.getBlockState(origin).is(Blocks.MUD) ? origin : origin.below();
        if (!level.getBlockState(seed).is(Blocks.MUD)) {
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
            if (!level.getBlockState(pos).is(Blocks.MUD)) {
                continue;
            }
            level.setBlock(pos, bogEarth, 2);
            placed++;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next.asLong())
                        && level.getBlockState(next).is(Blocks.MUD)
                        && random.nextFloat() < config.spreadChance()) {
                    queue.add(next.immutable());
                }
            }
        }
        return placed > 0;
    }
}
