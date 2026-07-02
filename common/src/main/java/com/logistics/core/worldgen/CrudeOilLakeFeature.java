package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

/**
 * Vanilla {@link LakeFeature} (crude oil as the fluid) with one tweak: the bank material is chosen by
 * biome. Extending the lake feature means we inherit all of its placement/carve/containment validation
 * (the reason it never hangs off cliffs or spawns mid-water) rather than re-implementing it; the
 * configured feature's own barrier is ignored. Each bank is a weighted mix of the biome's oil ore and its
 * natural ground, so the banks read as oil-soaked patches rather than a full ore casing. Add a line to
 * {@link #BANK} to give a biome family its own mix.
 */
public class CrudeOilLakeFeature extends LakeFeature {

    // Biome tag -> bank material, checked in order — earlier entries win, so put narrower tags first
    // (badlands is also in c:is_sandy but wants red sand). Unmatched biomes fall back to DEFAULT_BANK.
    private static final Map<TagKey<Biome>, Supplier<BlockStateProvider>> BANK = new LinkedHashMap<>();

    static {
        BANK.put(biomeTag("is_badlands"), mix(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 3, 1));
        BANK.put(biomeTag("is_sandy"), mix(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 3, 1)); // desert, beach, ...
    }

    private static final Supplier<BlockStateProvider> DEFAULT_BANK =
        mix(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 3, 1);

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
        for (Map.Entry<TagKey<Biome>, Supplier<BlockStateProvider>> entry : BANK.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue().get();
            }
        }
        return DEFAULT_BANK.get();
    }

    /** A bank that mixes the (lazily-resolved) oil ore and a natural block at the given weights. */
    private static Supplier<BlockStateProvider> mix(Supplier<Block> ore, Block natural, int oreWeight, int naturalWeight) {
        return () -> new WeightedStateProvider(WeightedList.<BlockState>builder()
            .add(ore.get().defaultBlockState(), oreWeight)
            .add(natural.defaultBlockState(), naturalWeight));
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
