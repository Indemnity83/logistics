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
 * A standalone oil-sands deposit: an irregular blob of a semi-random radius centred on the placement point,
 * filling solid ground (air and fluids are skipped, so it follows the terrain rather than floating) with a
 * speckled oil-ore/natural mix. The ore + natural blocks are chosen by biome at the block switch below —
 * badlands variants use red sand, everything else (desert, beach, ...) uses plain sand. Capped to low
 * surface heights so deposits stay off hills and mesas.
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
        // Keep deposits at low elevations — off hills and high badlands mesas.
        if (origin.getY() > config.maxY()) {
            return false;
        }
        String biome = level.getBiome(origin).unwrapKey().map(key -> key.identifier().getPath()).orElse("");

        int min = Math.min(config.minRadius(), config.maxRadius());
        int max = Math.max(config.minRadius(), config.maxRadius());
        int radius = min + (max > min ? random.nextInt(max - min + 1) : 0);

        return forEachInBlob(random, radius, origin, pos -> replaceSolid(level, pos, bankBlock(biome, random)));
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
     * Applies {@code action} to every block position within roughly {@code radius} of {@code center},
     * wobbling the boundary with low-frequency sine waves (random phase per deposit) so the blob is lumpy
     * rather than a clean sphere; returns whether the action reported a change at any of them.
     */
    private static boolean forEachInBlob(RandomSource random, int radius, BlockPos center, Predicate<BlockPos> action) {
        double amplitude = radius * 0.15; // how far the surface bulges/dents from a true sphere
        double frequency = 0.5;           // low -> smooth lumps rather than noise
        double phaseX = random.nextDouble() * Math.PI * 2.0;
        double phaseY = random.nextDouble() * Math.PI * 2.0;
        double phaseZ = random.nextDouble() * Math.PI * 2.0;
        int bound = radius + (int) Math.ceil(amplitude * 3.0);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -bound; dx <= bound; dx++) {
            for (int dy = -bound; dy <= bound; dy++) {
                for (int dz = -bound; dz <= bound; dz++) {
                    double edge = radius + amplitude * (Math.sin(dx * frequency + phaseX)
                            + Math.sin(dy * frequency + phaseY)
                            + Math.sin(dz * frequency + phaseZ));
                    if (dx * dx + dy * dy + dz * dz <= edge * edge) {
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
