package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A standalone oil-sands deposit: an irregular blob of a semi-random radius centred on the placement point,
 * filling solid ground (air and fluids are skipped, so it follows the terrain rather than floating) with a
 * speckled oil-ore/natural mix chosen by biome (see {@link OilOreMix}). Mostly ore with some natural ground
 * mixed in. Capped to low surface heights so deposits stay off hills and mesas.
 */
public record OilSandsFeature(int minRadius, int maxRadius, int maxY) implements Feature {

    public static final MapCodec<OilSandsFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 48).fieldOf("min_radius").forGetter(OilSandsFeature::minRadius),
            Codec.intRange(1, 48).fieldOf("max_radius").forGetter(OilSandsFeature::maxRadius),
            Codec.intRange(-64, 320).fieldOf("max_y").forGetter(OilSandsFeature::maxY))
        .apply(instance, OilSandsFeature::new));

    // Oil-sands banks are mostly ore with some natural ground mixed in.
    private static final int ORE_WEIGHT = 3;
    private static final int NATURAL_WEIGHT = 2;

    private static final double FREQUENCY = 0.5; // low -> smooth lumps rather than noise
    private static final double AMPLITUDE_FACTOR = 0.15; // how far the surface bulges/dents from a true sphere

    @Override
    public MapCodec<OilSandsFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // Keep deposits at low elevations — off hills and high badlands mesas.
        if (origin.getY() > maxY) {
            return false;
        }
        OilOreMix mix = OilOreMix.forBiome(level, origin);

        int min = Math.min(minRadius, maxRadius);
        int max = Math.max(minRadius, maxRadius);
        int radius = min + (max > min ? random.nextInt(max - min + 1) : 0);

        return forEachInBlob(random, radius, origin,
                pos -> replaceSolid(level, pos, mix.roll(random, ORE_WEIGHT, NATURAL_WEIGHT)));
    }

    /** Sets {@code state} only if the block is solid ground — leaves air (so the blob follows the
     * terrain) and fluids untouched. Returns whether it replaced anything. */
    private static boolean replaceSolid(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.isEmptyBlock(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        level.setBlock(pos, state, 2);
        return true;
    }

    /**
     * Applies {@code action} to every block position within roughly {@code radius} of {@code center}. The
     * boundary is wobbled by low-frequency sine waves (random phase per deposit) so the blob is lumpy rather
     * than a clean sphere. Returns whether the action reported a change at any position.
     */
    private static boolean forEachInBlob(RandomSource random, int radius, BlockPos center, Predicate<BlockPos> action) {
        double tau = Math.PI * 2.0;
        Blob blob = new Blob(radius, radius * AMPLITUDE_FACTOR,
                random.nextDouble() * tau, random.nextDouble() * tau, random.nextDouble() * tau);
        int bound = radius + (int) Math.ceil(blob.amplitude() * 3.0);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -bound; dx <= bound; dx++) {
            for (int dy = -bound; dy <= bound; dy++) {
                for (int dz = -bound; dz <= bound; dz++) {
                    any |= visit(blob, center, dx, dy, dz, pos, action);
                }
            }
        }
        return any;
    }

    /** Visits one offset: if it's inside the wobbled blob, points {@code pos} at it and runs {@code action}. */
    private static boolean visit(Blob blob, BlockPos center, int dx, int dy, int dz,
            BlockPos.MutableBlockPos pos, Predicate<BlockPos> action) {
        if (!blob.contains(dx, dy, dz)) {
            return false;
        }
        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
        return action.test(pos);
    }

    /** The lumpy sphere: a base {@code radius} perturbed per-axis by sine waves at {@code amplitude}. */
    private record Blob(int radius, double amplitude, double phaseX, double phaseY, double phaseZ) {
        boolean contains(int dx, int dy, int dz) {
            double edge = radius + amplitude * (Math.sin(dx * FREQUENCY + phaseX)
                    + Math.sin(dy * FREQUENCY + phaseY)
                    + Math.sin(dz * FREQUENCY + phaseZ));
            return (double) dx * dx + dy * dy + dz * dz <= edge * edge;
        }
    }
}
