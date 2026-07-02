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
 * The {@link Bank} mix (an oil ore blended with a natural block by weight; {@link Bank#roll} yields one or
 * the other per block) plus the crude oil lake's biome-tag lookup. Lake banks are mostly natural — a thin
 * oil crust. The oil-sands deposit keeps its own per-biome lookup in {@link OilSandsFeature}.
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

    // Crude oil lake banks: mostly natural ground with oil ore poking through. Checked in order — earlier
    // wins, so narrower tags first (badlands is also in c:is_sandy but wants red sand). LAKE_DEFAULT covers
    // every other overworld biome the lakes reach.
    private static final Map<TagKey<Biome>, Bank> LAKE = new LinkedHashMap<>();
    private static final Bank LAKE_DEFAULT = new Bank(() -> LogisticsCore.BLOCK.OIL_SHALE, Blocks.GRAVEL, 1, 5);

    static {
        LAKE.put(biomeTag("is_badlands"), new Bank(() -> LogisticsCore.BLOCK.OIL_RED_SAND, Blocks.RED_SAND, 1, 3));
        LAKE.put(biomeTag("is_sandy"), new Bank(() -> LogisticsCore.BLOCK.OIL_SAND, Blocks.SAND, 1, 3));
    }

    public static Bank lakeBank(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (Map.Entry<TagKey<Biome>, Bank> entry : LAKE.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return LAKE_DEFAULT;
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceId.in("c", path).toIdentifier());
    }
}
