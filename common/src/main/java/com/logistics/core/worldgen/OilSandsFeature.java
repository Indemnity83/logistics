package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
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
        int radiusSq = radius * radius;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    // Only convert solid ground; leave air (so the blob follows the terrain) and fluids.
                    if (level.isEmptyBlock(pos) || !level.getFluidState(pos).isEmpty()) {
                        continue;
                    }
                    switch (biome) {
                        case "badlands", "wooded_badlands", "eroded_badlands" ->
                            level.setBlock(pos, mix(random, LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 3, 2), 2);
                        default ->
                            level.setBlock(pos, mix(random, LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 3, 2), 2);
                    }
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Picks the first block against the second at the given weight ratio — a per-block speckle. */
    private static BlockState mix(RandomSource random, Block first, Block second, int firstWeight, int secondWeight) {
        return random.nextInt(firstWeight + secondWeight) < firstWeight
                ? first.defaultBlockState()
                : second.defaultBlockState();
    }
}
