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

/**
 * A biome-appropriate oil ore and the natural ground it speckles into. {@link #forBiome} picks the pair by
 * conventional biome tag (oil red sand in badlands, oil sand in other sandy biomes, oil shale elsewhere);
 * {@link #roll} blends the two at a ratio the caller supplies. Shared by the crude oil seeps
 * ({@link OilSeepFeature}) and the oil sands deposits ({@link OilSandsFeature}), which pass their own ratios.
 */
public record OilOreMix(Supplier<Block> ore, Block natural) {

    /** Picks {@code ore} against {@code natural} at the given weight ratio — a per-block speckle. */
    public BlockState roll(RandomSource random, int oreWeight, int naturalWeight) {
        return random.nextInt(oreWeight + naturalWeight) < oreWeight
                ? ore.get().defaultBlockState()
                : natural.defaultBlockState();
    }

    // Biome tag -> ore/natural pair, checked in order — earlier wins, so narrower tags first (badlands is
    // also in c:is_sandy but wants red sand). DEFAULT covers every other biome (e.g. the overworld seeps).
    private static final Map<TagKey<Biome>, OilOreMix> BY_BIOME = new LinkedHashMap<>();
    private static final OilOreMix DEFAULT = new OilOreMix(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL);

    static {
        BY_BIOME.put(biomeTag("is_badlands"), new OilOreMix(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND));
        BY_BIOME.put(biomeTag("is_sandy"), new OilOreMix(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND));
    }

    public static OilOreMix forBiome(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, OilOreMix> entry : BY_BIOME.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT;
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
