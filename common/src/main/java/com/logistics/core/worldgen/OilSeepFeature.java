package com.logistics.core.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Vanilla {@link LakeFeature} (crude oil as the fluid) with the bank material chosen by biome (see
 * {@link OilOreMix}). Extending the lake feature inherits all of its placement/carve/containment validation
 * (why it never hangs off cliffs or spawns mid-water) rather than re-implementing it. The lake samples its
 * barrier provider only once per lake, so a weighted provider can't give a per-block mix; instead we lay a
 * solid oil-ore barrier and then {@link #speckle} it — re-rolling each placed ore block through the mix so
 * the banks read as oil-soaked patches rather than a full casing.
 *
 * <p>The configured feature's own {@code barrier} is ignored at runtime: {@link #place} replaces it with
 * the biome's oil ore before delegating to the lake feature.
 */
public class OilSeepFeature extends LakeFeature {

    // Lake banks are mostly natural — a thin oil crust.
    private static final int ORE_WEIGHT = 1;
    private static final int NATURAL_WEIGHT = 3;

    public OilSeepFeature() {
        super(LakeFeature.Configuration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<LakeFeature.Configuration> context) {
        OilOreMix mix = OilOreMix.forBiome(context.level(), context.origin());
        LakeFeature.Configuration base = context.config();
        LakeFeature.Configuration withOreBarrier = new LakeFeature.Configuration(
                base.fluid(),
                BlockStateProvider.simple(mix.ore().get()));
        FeaturePlaceContext<LakeFeature.Configuration> withBarrier = new FeaturePlaceContext<>(
                context.topFeature(), context.level(), context.chunkGenerator(),
                context.random(), context.origin(), withOreBarrier);
        boolean placed = super.place(withBarrier);
        if (placed) {
            speckle(context.level(), context.random(), context.origin(), mix);
        }
        return placed;
    }

    /**
     * Break up the solid ore barrier: the lake carves a 16x16x8 volume anchored at {@code origin} - (8,4,8)
     * with the barrier a block beyond, so scan that neighbourhood and re-roll each of our ore blocks through
     * the mix, giving a speckled bank rather than a full casing.
     */
    private static void speckle(WorldGenLevel level, RandomSource random, BlockPos origin, OilOreMix mix) {
        Block ore = mix.ore().get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -9; dx <= 8; dx++) {
            for (int dy = -5; dy <= 4; dy++) {
                for (int dz = -9; dz <= 8; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(pos).is(ore)) {
                        level.setBlock(pos, mix.roll(random, ORE_WEIGHT, NATURAL_WEIGHT), 2);
                    }
                }
            }
        }
    }
}
