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
 * Biome -> oil-ore "bank" mixes, shared by the crude oil lake banks ({@link CrudeOilLakeFeature}) and the
 * standalone oil deposit ({@link OilDepositFeature}). Each bank blends an oil ore with a natural block by
 * weight; {@link Bank#roll} yields one or the other so callers get a speckled, per-block mix.
 */
public final class OilBanks {

    private OilBanks() {}

    /** An oil-ore bank blended with a natural block; {@code roll} picks natural at its weight share. */
    public record Bank(Supplier<Block> ore, Block natural, int oreWeight, int naturalWeight) {
        public BlockState roll(RandomSource random) {
            float naturalShare = (float) naturalWeight / (oreWeight + naturalWeight);
            return random.nextFloat() < naturalShare ? natural.defaultBlockState() : ore.get().defaultBlockState();
        }
    }

    // Checked in order — earlier wins, so narrower tags first (badlands is also in c:is_sandy but wants
    // red sand). DEFAULT is only reached by the lake, which spawns in every biome; the deposit is limited
    // to c:is_sandy, so it only ever resolves the badlands/sandy banks.
    private static final Map<TagKey<Biome>, Bank> BANKS = new LinkedHashMap<>();

    static {
        BANKS.put(biomeTag("is_badlands"), new Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 1, 3));
        BANKS.put(biomeTag("is_sandy"), new Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 1, 3)); // desert, beach
    }

    private static final Bank DEFAULT = new Bank(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 1, 5);

    public static Bank forBiome(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, Bank> entry : BANKS.entrySet()) {
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
