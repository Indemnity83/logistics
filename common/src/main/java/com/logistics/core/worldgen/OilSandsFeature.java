package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A standalone oil-sands deposit: a sphere of a semi-random radius centred on the placement point, filling
 * solid ground (air and fluids are skipped, so it follows the terrain rather than floating) with a
 * speckled oil-ore/natural mix. The ore + natural blocks are a static per-biome lookup ({@link #BY_BIOME});
 * biomes it's injected into but doesn't list (e.g. beach) fall back to plain oil sand.
 */
public class OilSandsFeature extends Feature<OilSandsConfiguration> {

    private static final OilBanks.Bank OIL_SAND =
        new OilBanks.Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 3, 2);
    private static final OilBanks.Bank OIL_RED_SAND =
        new OilBanks.Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 3, 2);

    // Specific biome -> bank. Add a line to give a biome its own ore/natural blocks; unlisted biomes
    // (e.g. beach) fall back to OIL_SAND.
    private static final Map<ResourceKey<Biome>, OilBanks.Bank> BY_BIOME = Map.of(
        Biomes.DESERT, OIL_SAND,
        Biomes.BADLANDS, OIL_RED_SAND,
        Biomes.WOODED_BADLANDS, OIL_RED_SAND,
        Biomes.ERODED_BADLANDS, OIL_RED_SAND);

    public OilSandsFeature(Codec<OilSandsConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSandsConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilSandsConfiguration config = context.config();
        BlockPos origin = context.origin();
        OilBanks.Bank bank = bankFor(level, origin);

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
                    if (!level.isEmptyBlock(pos) && level.getFluidState(pos).isEmpty()) {
                        level.setBlock(pos, bank.roll(random), 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    private static OilBanks.Bank bankFor(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> BY_BIOME.getOrDefault(key, OIL_SAND)).orElse(OIL_SAND);
    }
}
