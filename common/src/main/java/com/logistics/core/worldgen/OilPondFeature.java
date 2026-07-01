package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Carves a large, irregular "pond" of oil-soaked ground. Over a lobed disc around the (surface) origin it
 * walks each column down and turns any {@code target} block (sand + sandstone, or the red variants) into
 * the config's {@code result}. The outline is perturbed with two sine lobes plus per-column jitter, the
 * depth tapers from a deep centre to a shallow rim, and each column follows the local surface — so ponds
 * vary in size and shape and never read as a flat disc. Only target blocks are replaced, so the pond
 * bottoms out on the stone beneath the sand — unlike vanilla's {@code disk}, whose half-height caps at 4.
 */
public class OilPondFeature extends Feature<OilPondConfiguration> {

    public OilPondFeature(Codec<OilPondConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilPondConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilPondConfiguration config = context.config();
        BlockPredicate target = config.target();
        BlockState result = config.result();
        // Jitter the configured base size per pond (±2) so neighbouring ponds differ in scale.
        int radius = Math.max(2, config.radius() + random.nextInt(5) - 2);
        int maxDepth = Math.max(2, config.depth() + random.nextInt(5) - 2);

        BlockPos origin = context.origin();
        int surfaceY = origin.getY() - 1;

        // Per-pond edge lobes so no two ponds share an outline.
        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;
        double amp1 = radius * 0.28;
        double amp2 = radius * 0.14;
        int scan = radius + (int) Math.ceil(amp1 + amp2) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dz = -scan; dz <= scan; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                double angle = Math.atan2(dz, dx);
                double edge = radius
                        + Math.sin(angle * 3.0 + phase1) * amp1
                        + Math.sin(angle * 5.0 + phase2) * amp2
                        + (random.nextFloat() - 0.5f);
                if (dist > edge) {
                    continue;
                }
                double nd = dist / Math.max(1.0, edge);
                int columnDepth = Math.max(1, (int) Math.round(maxDepth * (1.0 - nd * nd * 0.5)));

                // Follow the local surface (dunes) rather than carving from one flat plane.
                int localTop = Integer.MIN_VALUE;
                for (int y = surfaceY + 3; y >= surfaceY - maxDepth; y--) {
                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    if (target.test(level, pos)) {
                        localTop = y;
                        break;
                    }
                }
                if (localTop == Integer.MIN_VALUE) {
                    continue;
                }
                for (int dy = 0; dy < columnDepth; dy++) {
                    pos.set(origin.getX() + dx, localTop - dy, origin.getZ() + dz);
                    if (target.test(level, pos)) {
                        level.setBlock(pos, result, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }
}
