package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A standalone oil-sands deposit: a sphere of a semi-random radius centred on the placement point, filling
 * solid ground (air and fluids are skipped, so it follows the terrain rather than floating) with a
 * speckled oil-ore/natural mix. The ore + natural blocks are chosen by biome at the block switch below —
 * badlands variants use red sand, everything else (desert, beach, ...) uses plain sand.
 */
public class OilSandsFeature extends Feature<OilSandsConfiguration> {

    public OilSandsFeature(Codec<OilSandsConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSandsConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilSandsConfiguration config = context.config();
        BlockPos origin = context.origin();
        String biome = level.getBiome(origin).unwrapKey().map(key -> key.identifier().getPath()).orElse("");

        int min = Math.min(config.minRadius(), config.maxRadius());
        int max = Math.max(config.minRadius(), config.maxRadius());
        int radius = min + (max > min ? random.nextInt(max - min + 1) : 0);

        return forEachInSphere(radius, origin, pos -> replaceSolid(level, pos, bankBlock(biome, random)));
    }

    /** Sets {@code state} only if the block is solid ground — leaves air (so the blob follows the
     * terrain) and fluids untouched. Returns whether it replaced anything. */
    private static boolean replaceSolid(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.isEmptyBlock(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        level.setBlock(pos, state, 2);
        return true;
    }

    /**
     * Applies {@code action} to every block position within {@code radius} of {@code center} (a solid
     * sphere); returns whether the action reported a change at any of them.
     */
    private static boolean forEachInSphere(int radius, BlockPos center, Predicate<BlockPos> action) {
        int radiusSq = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        any |= action.test(pos);
                    }
                }
            }
        }
        return any;
    }

    /** The speckled bank block for a biome: badlands variants use red sand, everything else plain sand. */
    private static BlockState bankBlock(String biome, RandomSource random) {
        return switch (biome) {
            case "badlands", "wooded_badlands", "eroded_badlands" ->
                mix(random, LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 3, 2);
            default ->
                mix(random, LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 3, 2);
        };
    }

    /** Picks the first block against the second at the given weight ratio — a per-block speckle. */
    private static BlockState mix(RandomSource random, Block first, Block second, int firstWeight, int secondWeight) {
        return random.nextInt(firstWeight + secondWeight) < firstWeight
                ? first.defaultBlockState()
                : second.defaultBlockState();
    }

}
