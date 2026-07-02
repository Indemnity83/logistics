package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Vanilla {@link LakeFeature} (crude oil as the fluid) with one tweak: the bank ore is chosen by biome.
 * Extending the lake feature means we inherit all of its placement/carve/containment validation (the
 * reason it never hangs off cliffs or spawns mid-water) rather than re-implementing it; the configured
 * feature's own barrier is ignored. Add a line to {@link #BANK_ORE} to give a biome family its own ore.
 */
public class CrudeOilLakeFeature extends LakeFeature {

    // Biome tag -> bank ore, checked in order — earlier entries win, so put narrower tags first
    // (badlands is also in c:is_sandy but wants red sand). Unmatched biomes fall back to DEFAULT_ORE.
    private static final Map<TagKey<Biome>, Supplier<Block>> BANK_ORE = new LinkedHashMap<>();

    static {
        BANK_ORE.put(biomeTag("is_badlands"), () -> LogisticsCore.BLOCK.OIL_RED_SAND);
        BANK_ORE.put(biomeTag("is_sandy"), () -> LogisticsCore.BLOCK.OIL_SAND); // desert, beach, ...
    }

    private static final Supplier<Block> DEFAULT_ORE = () -> LogisticsCore.BLOCK.OIL_SHALE;

    public CrudeOilLakeFeature() {
        super(LakeFeature.Configuration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<LakeFeature.Configuration> context) {
        LakeFeature.Configuration base = context.config();
        LakeFeature.Configuration withBiomeBarrier = new LakeFeature.Configuration(
                base.fluid(),
                biomeBarrier(context.level(), context.origin()),
                base.canPlaceFeature(),
                base.canReplaceWithAirOrFluid(),
                base.canReplaceWithBarrier());
        FeaturePlaceContext<LakeFeature.Configuration> withBarrier = new FeaturePlaceContext<>(
                context.topFeature(), context.level(), context.chunkGenerator(),
                context.random(), context.origin(), withBiomeBarrier);
        return super.place(withBarrier);
    }

    private static BlockStateProvider biomeBarrier(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, Supplier<Block>> entry : BANK_ORE.entrySet()) {
            if (biome.is(entry.getKey())) {
                return BlockStateProvider.simple(entry.getValue().get());
            }
        }
        return BlockStateProvider.simple(DEFAULT_ORE.get());
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
