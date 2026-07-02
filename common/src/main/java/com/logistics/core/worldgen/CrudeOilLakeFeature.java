package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Vanilla {@link LakeFeature} (crude oil as the fluid) with the bank material chosen by biome. Extending
 * the lake feature inherits all of its placement/carve/containment validation (why it never hangs off
 * cliffs or spawns mid-water) rather than re-implementing it. The lake samples its barrier provider only
 * once per lake, so a weighted provider can't give a per-block mix; instead we lay a solid oil-ore barrier
 * and then {@link #speckle} it — re-rolling each placed ore block against the biome's natural ground so
 * the banks read as oil-soaked patches rather than a full casing. Add a line to {@link #BANK} for a biome.
 */
public class CrudeOilLakeFeature extends LakeFeature {

    /** An oil-ore bank blended with natural ground; {@code natural} replaces the ore at its weight share. */
    private record Bank(Supplier<Block> ore, Block natural, int oreWeight, int naturalWeight) {
        float naturalShare() {
            return (float) naturalWeight / (oreWeight + naturalWeight);
        }
    }

    // Biome tag -> bank, checked in order — earlier entries win, so put narrower tags first (badlands is
    // also in c:is_sandy but wants red sand). Unmatched biomes fall back to DEFAULT_BANK.
    private static final Map<TagKey<Biome>, Bank> BANK = new LinkedHashMap<>();

    static {
        BANK.put(biomeTag("is_badlands"), new Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 1, 3));
        BANK.put(biomeTag("is_sandy"), new Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 1, 3)); // desert, beach
    }

    private static final Bank DEFAULT_BANK = new Bank(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 1, 5);

    public CrudeOilLakeFeature() {
        super(LakeFeature.Configuration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<LakeFeature.Configuration> context) {
        Bank bank = bankFor(context.level(), context.origin());
        LakeFeature.Configuration base = context.config();
        LakeFeature.Configuration withOreBarrier = new LakeFeature.Configuration(
                base.fluid(),
                BlockStateProvider.simple(bank.ore().get()),
                base.canPlaceFeature(),
                base.canReplaceWithAirOrFluid(),
                base.canReplaceWithBarrier());
        FeaturePlaceContext<LakeFeature.Configuration> withBarrier = new FeaturePlaceContext<>(
                context.topFeature(), context.level(), context.chunkGenerator(),
                context.random(), context.origin(), withOreBarrier);
        boolean placed = super.place(withBarrier);
        if (placed) {
            speckle(context.level(), context.random(), context.origin(), bank);
        }
        return placed;
    }

    private static Bank bankFor(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, Bank> entry : BANK.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_BANK;
    }

    /**
     * Break up the solid ore barrier: the lake carves a 16x16x8 volume anchored at {@code origin} - (8,4,8)
     * with the barrier a block beyond, so scan that neighbourhood and re-roll each of our ore blocks against
     * the natural block, giving a speckled bank rather than a full casing.
     */
    private static void speckle(WorldGenLevel level, RandomSource random, BlockPos origin, Bank bank) {
        Block ore = bank.ore().get();
        BlockState natural = bank.natural().defaultBlockState();
        float share = bank.naturalShare();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -9; dx <= 8; dx++) {
            for (int dy = -5; dy <= 4; dy++) {
                for (int dz = -9; dz <= 8; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(pos).is(ore) && random.nextFloat() < share) {
                        level.setBlock(pos, natural, 2);
                    }
                }
            }
        }
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
