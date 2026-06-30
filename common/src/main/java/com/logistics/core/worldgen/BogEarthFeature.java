package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Generates bog earth at a swamp's water edge: finds surface dirt/grass/mud that borders water (and is
 * not under standing water) near vegetation, then flood-fills outward through adjacent soil, each step
 * a {@code spread_chance} roll, capped at {@code max_count}. Spread is 3D, so it sinks into the bank.
 */
public class BogEarthFeature extends Feature<BogEarthConfiguration> {

    public BogEarthFeature(Codec<BogEarthConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BogEarthConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BogEarthConfiguration config = context.config();

        BlockPos seed = findSurfaceSoil(level, context.origin());
        if (seed == null || !isValidSeed(level, seed, config.plantRadius())) {
            return false;
        }

        BlockState bogEarth = LogisticsCore.BLOCK.BOG_EARTH.defaultBlockState();
        Set<Long> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        visited.add(seed.asLong());

        int placed = 0;
        while (!queue.isEmpty() && placed < config.maxCount()) {
            BlockPos pos = queue.poll();
            if (!isSoil(level.getBlockState(pos)) || isWaterAbove(level, pos)) {
                continue;
            }
            setBlock(level, pos, bogEarth);
            placed++;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next.asLong())
                        && isSoil(level.getBlockState(next))
                        && random.nextFloat() < config.spreadChance()) {
                    queue.add(next.immutable());
                }
            }
        }
        return placed > 0;
    }

    /** Scans down from the surface heightmap to the first dirt/grass/mud, or null if none is close. */
    private static BlockPos findSurfaceSoil(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int i = 0; i < 12; i++) {
            if (isSoil(level.getBlockState(cursor))) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean isValidSeed(WorldGenLevel level, BlockPos seed, int plantRadius) {
        return !isWaterAbove(level, seed) && touchesWater(level, seed) && hasPlantNearby(level, seed, plantRadius);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MUD);
    }

    private static boolean isWaterAbove(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    /** True when water is horizontally adjacent at the seed's level or one above (the shoreline). */
    private static boolean touchesWater(WorldGenLevel level, BlockPos seed) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(seed.relative(dir)).is(FluidTags.WATER)
                    || level.getFluidState(seed.above().relative(dir)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPlantNearby(WorldGenLevel level, BlockPos seed, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                seed.offset(-radius, -1, -radius), seed.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.LOGS)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SMALL_FLOWERS)) {
                return true;
            }
        }
        return false;
    }
}
