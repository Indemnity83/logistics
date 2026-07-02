package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A standalone oil-sands deposit: a sphere of a semi-random radius centred on the placement point, filling
 * solid ground (air and fluids are skipped, so it follows the terrain rather than floating) with the
 * biome's speckled oil-ore/natural bank ({@link OilBanks#sandsBank}). Uses the same ore/natural blocks as
 * the crude oil lake banks but its own, ore-heavier mix.
 */
public class OilSandsFeature extends Feature<OilSandsConfiguration> {

    public OilSandsFeature(Codec<OilSandsConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSandsConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilSandsConfiguration config = context.config();
        BlockPos origin = context.origin();
        OilBanks.Bank bank = OilBanks.sandsBank(level, origin);

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
}
