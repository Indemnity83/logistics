package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A surface crude oil "seep": an organic bowl filled with crude oil, its floor and walls lined with the
 * biome's oil ore (so the pool is contained and reads as oil-soaked banks), plus a few ore tendrils that
 * random-walk outward from the rim and peter out on a per-step probability. Mirrors the shape machinery
 * of {@link OilPondFeature} (lobed edge, jittered size) but centres on a fluid pool rather than solid ore.
 */
public class OilSeepFeature extends Feature<OilSeepConfiguration> {

    private static final Direction[] WALL_DIRS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public OilSeepFeature(Codec<OilSeepConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSeepConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilSeepConfiguration config = context.config();
        BlockState fluid = config.fluid();
        BlockState shell = config.shell();
        int radius = Math.max(2, config.radius() + random.nextInt(3) - 1);
        int depth = Math.max(1, config.depth() + random.nextInt(3) - 1);

        BlockPos origin = context.origin();
        int surfaceY = origin.getY() - 1;

        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;
        double amp1 = radius * 0.3;
        double amp2 = radius * 0.15;

        // Fill an organic bowl (deep centre, shallow rim) with crude oil.
        Set<Long> oil = new HashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                double angle = Math.atan2(dz, dx);
                double edge = radius
                        + Math.sin(angle * 3.0 + phase1) * amp1
                        + Math.sin(angle * 5.0 + phase2) * amp2;
                if (dist > edge) {
                    continue;
                }
                double nd = dist / Math.max(1.0, edge);
                int columnDepth = Math.max(1, (int) Math.round(depth * (1.0 - nd * nd)));
                for (int dy = 0; dy < columnDepth; dy++) {
                    pos.set(origin.getX() + dx, surfaceY - dy, origin.getZ() + dz);
                    level.setBlock(pos, fluid, 2);
                    oil.add(pos.asLong());
                }
            }
        }
        if (oil.isEmpty()) {
            return false;
        }

        // Line the basin: every non-top face of an oil block that isn't oil becomes shell, so the pool is
        // contained and the walls read as oil-soaked ground.
        for (long packed : oil) {
            BlockPos oilPos = BlockPos.of(packed);
            for (Direction dir : WALL_DIRS) {
                BlockPos wall = oilPos.relative(dir);
                if (!oil.contains(wall.asLong())) {
                    level.setBlock(wall, shell, 2);
                }
            }
        }

        // Tendrils: a few ore streaks that walk outward from the rim and fade on tendrilChance.
        int tendrils = 2 + random.nextInt(4);
        for (int t = 0; t < tendrils; t++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dxStep = Math.cos(angle);
            double dzStep = Math.sin(angle);
            for (int step = radius; step < radius + 8; step++) {
                int x = origin.getX() + (int) Math.round(dxStep * step);
                int z = origin.getZ() + (int) Math.round(dzStep * step);
                int groundY = surfaceOf(level, x, z, surfaceY + 3, surfaceY - depth - 3);
                if (groundY == Integer.MIN_VALUE) {
                    break;
                }
                pos.set(x, groundY, z);
                level.setBlock(pos, shell, 2);
                if (random.nextFloat() >= config.tendrilChance()) {
                    break;
                }
            }
        }
        return true;
    }

    /** Top solid ground Y in a column between {@code fromY} (down) and {@code toY}, or MIN_VALUE if none. */
    private static int surfaceOf(WorldGenLevel level, int x, int z, int fromY, int toY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = fromY; y >= toY; y--) {
            pos.set(x, y, z);
            if (!level.isEmptyBlock(pos) && level.getFluidState(pos).isEmpty()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
