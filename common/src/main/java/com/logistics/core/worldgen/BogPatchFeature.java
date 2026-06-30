package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Like vanilla's disk, but irregular: it unions 2–4 overlapping circular lobes (jittered centre + radius
 * within the configured radius) so the patch outline is lumpy rather than a clean circle. Each resulting
 * column has its surface dirt/grass/mud (within {@code half_height}) swapped to the configured block.
 */
public class BogPatchFeature extends Feature<BogPatchConfiguration> {

    public BogPatchFeature(Codec<BogPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BogPatchConfiguration> context) {
        BogPatchConfiguration config = context.config();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        int maxRadius = config.radius().sample(random);
        if (maxRadius < 1) {
            return false;
        }

        // Build an irregular column footprint from a handful of overlapping lobes.
        Set<Long> columns = new HashSet<>();
        int lobes = 2 + random.nextInt(3);
        for (int i = 0; i < lobes; i++) {
            int offsetX = random.nextInt(maxRadius + 1) - maxRadius / 2;
            int offsetZ = random.nextInt(maxRadius + 1) - maxRadius / 2;
            int lobeRadius = 2 + random.nextInt(Math.max(1, maxRadius - 1));
            int cx = origin.getX() + offsetX;
            int cz = origin.getZ() + offsetZ;
            for (int dx = -lobeRadius; dx <= lobeRadius; dx++) {
                for (int dz = -lobeRadius; dz <= lobeRadius; dz++) {
                    if (dx * dx + dz * dz <= lobeRadius * lobeRadius) {
                        columns.add(pack(cx + dx, cz + dz));
                    }
                }
            }
        }

        int top = origin.getY() + config.halfHeight();
        int bottom = origin.getY() - config.halfHeight() - 1;
        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long column : columns) {
            placedAny |= placeColumn(config, level, random, top, bottom, pos.set(unpackX(column), 0, unpackZ(column)));
        }
        return placedAny;
    }

    private boolean placeColumn(BogPatchConfiguration config, WorldGenLevel level, RandomSource random,
            int top, int bottom, BlockPos.MutableBlockPos pos) {
        boolean placedAny = false;
        for (int y = top; y > bottom; y--) {
            pos.setY(y);
            if (config.target().test(level, pos)) {
                BlockState state = config.stateProvider().getOptionalState(level, random, pos);
                if (state != null) {
                    level.setBlock(pos, state, 2);
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }

    private static long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }
}
