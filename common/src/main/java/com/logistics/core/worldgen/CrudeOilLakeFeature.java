package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Vanilla {@link LakeFeature} (crude oil as the fluid) with one tweak: the barrier is chosen by biome —
 * oil sand in deserts, oil red sand in badlands, oil shale elsewhere. Extending the lake feature means we
 * inherit all of its placement/carve/containment validation (the reason it never hangs off cliffs or
 * spawns mid-water), rather than re-implementing it. The configured feature's own barrier is ignored.
 */
public class CrudeOilLakeFeature extends LakeFeature {

    private static final TagKey<Biome> IS_DESERT =
        TagKey.create(Registries.BIOME, ResourceId.in("c", "is_desert").toIdentifier());
    private static final TagKey<Biome> IS_BADLANDS =
        TagKey.create(Registries.BIOME, ResourceId.in("c", "is_badlands").toIdentifier());

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
        if (biome.is(IS_DESERT)) {
            return BlockStateProvider.simple(LogisticsCore.BLOCK.OIL_SAND);
        }
        if (biome.is(IS_BADLANDS)) {
            return BlockStateProvider.simple(LogisticsCore.BLOCK.OIL_RED_SAND);
        }
        return BlockStateProvider.simple(LogisticsCore.BLOCK.OIL_SHALE);
    }
}
