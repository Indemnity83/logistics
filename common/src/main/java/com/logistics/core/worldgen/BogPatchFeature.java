package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Turns soil into bog earth in organic patches: seeds on the surface block at the origin (whatever
 * {@code target} matches — mud in mangrove swamps, grass/dirt at regular-swamp water edges), then
 * flood-fills (6-neighbour, 3D) through adjacent matching blocks, each step a {@code spreadChance}
 * roll, capped at {@code maxCount}. Only {@code target} blocks are ever replaced.
 */
public record BogPatchFeature(BlockPredicate target, float spreadChance, int maxCount) implements Feature {

    public static final MapCodec<BogPatchFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPredicate.CODEC.fieldOf("target").forGetter(BogPatchFeature::target),
            Codec.floatRange(0.0f, 1.0f).fieldOf("spread_chance").forGetter(BogPatchFeature::spreadChance),
            Codec.intRange(1, 4096).fieldOf("max_count").forGetter(BogPatchFeature::maxCount))
        .apply(instance, BogPatchFeature::new));

    @Override
    public MapCodec<BogPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // The heightmap origin sits just above the surface; the soil is the block below it.
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
        while (!queue.isEmpty() && placed < maxCount) {
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
                        && random.nextFloat() < spreadChance) {
                    queue.add(next.immutable());
                }
            }
        }
        return placed > 0;
    }
}
