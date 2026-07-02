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
 * Biome -> oil-ore "bank" mixes. Each bank blends an oil ore with a natural block by weight;
 * {@link Bank#roll} yields one or the other so callers get a speckled, per-block mix. The two callers use
 * different ratios: {@link #lakeBank} makes crude oil lake banks mostly natural (a thin oil crust), while
 * {@link #sandsBank} makes the standalone oil-sands deposits mostly ore. The ore/natural block per biome is
 * the same in both; only the weights differ.
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

    // Crude oil lake banks: mostly natural ground with oil ore poking through.
    private static final Map<TagKey<Biome>, Bank> LAKE = new LinkedHashMap<>();
    private static final Bank LAKE_DEFAULT = new Bank(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 1, 5);

    // Oil-sands deposits: mostly oil ore with some natural ground mixed in.
    private static final Map<TagKey<Biome>, Bank> SANDS = new LinkedHashMap<>();
    private static final Bank SANDS_DEFAULT = new Bank(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 3, 1);

    static {
        // Checked in order — earlier wins, so narrower tags first (badlands is also in c:is_sandy but wants
        // red sand). The *_DEFAULT is only reached by lakes (they spawn everywhere); oil sands are limited
        // to c:is_sandy, so they only ever resolve the badlands/sandy banks.
        LAKE.put(biomeTag("is_badlands"), new Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 1, 3));
        LAKE.put(biomeTag("is_sandy"), new Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 1, 3));

        SANDS.put(biomeTag("is_badlands"), new Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 3, 2));
        SANDS.put(biomeTag("is_sandy"), new Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 3, 2));
    }

    public static Bank lakeBank(WorldGenLevel level, BlockPos pos) {
        return resolve(LAKE, LAKE_DEFAULT, level, pos);
    }

    public static Bank sandsBank(WorldGenLevel level, BlockPos pos) {
        return resolve(SANDS, SANDS_DEFAULT, level, pos);
    }

    private static Bank resolve(Map<TagKey<Biome>, Bank> banks, Bank fallback, WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, Bank> entry : banks.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
