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
 * biome's oil ore (so the pool is contained and reads as oil-soaked banks), plus ore tendrils. Each
 * column on the pool's perimeter has a small chance to seed a tendril, which then spreads outward — the
 * outward direction is recomputed from the pool centre every step, so tendrils always crawl away from the
 * lake with a small wiggle, fading on a per-step probability. Mirrors {@link OilPondFeature}'s lobed edge.
 */
public class OilSeepFeature extends Feature<OilSeepConfiguration> {

    private static final Direction[] WALL_DIRS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

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
        if (unsuitable(level, origin, radius, depth)) {
            return false;
        }
        int surfaceY = origin.getY() - 1;

        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;
        double amp1 = radius * 0.3;
        double amp2 = radius * 0.15;

        // Fill an organic bowl (deep centre, shallow rim) with crude oil, tracking the filled columns.
        Set<Long> oil = new HashSet<>();
        Set<Long> columns = new HashSet<>();
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
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                columns.add(column(x, z));
                double nd = dist / Math.max(1.0, edge);
                int columnDepth = Math.max(1, (int) Math.round(depth * (1.0 - nd * nd)));
                for (int dy = 0; dy < columnDepth; dy++) {
                    pos.set(x, surfaceY - dy, z);
                    level.setBlock(pos, fluid, 2);
                    oil.add(pos.asLong());
                }
            }
        }
        if (oil.isEmpty()) {
            return false;
        }

        // Line the basin: every non-top face of an oil block that isn't oil becomes shell.
        for (long packed : oil) {
            BlockPos oilPos = BlockPos.of(packed);
            for (Direction dir : WALL_DIRS) {
                BlockPos wall = oilPos.relative(dir);
                if (!oil.contains(wall.asLong())) {
                    level.setBlock(wall, shell, 2);
                }
            }
        }

        placeTendrils(level, random, config, columns, origin, surfaceY, radius, depth, shell);
        return true;
    }

    /** Seeds tendrils on perimeter columns and spreads each outward from the pool centre. */
    private void placeTendrils(WorldGenLevel level, RandomSource random, OilSeepConfiguration config,
            Set<Long> columns, BlockPos origin, int surfaceY, int radius, int depth, BlockState shell) {
        double centerX = origin.getX() + 0.5;
        double centerZ = origin.getZ() + 0.5;
        int fromY = surfaceY + 3;
        int toY = surfaceY - depth - 3;

        // Perimeter = columns just outside the filled pool.
        Set<Long> perimeter = new HashSet<>();
        for (long packed : columns) {
            int x = (int) (packed >> 32);
            int z = (int) packed;
            for (int[] d : CARDINALS) {
                long neighbor = column(x + d[0], z + d[1]);
                if (!columns.contains(neighbor)) {
                    perimeter.add(neighbor);
                }
            }
        }

        for (long packed : perimeter) {
            if (random.nextFloat() >= config.perimeterChance()) {
                continue;
            }
            double fx = (int) (packed >> 32) + 0.5;
            double fz = (int) packed + 0.5;
            for (int step = 0; step <= radius; step++) {
                int gy = surfaceOf(level, (int) Math.floor(fx), (int) Math.floor(fz), fromY, toY);
                if (gy != Integer.MIN_VALUE) {
                    level.setBlock(new BlockPos((int) Math.floor(fx), gy, (int) Math.floor(fz)), shell, 2);
                }
                if (step == radius || random.nextFloat() >= config.tendrilChance()) {
                    break;
                }
                // Always step away from the pool centre, with a small wiggle for an organic curl.
                double outward = Math.atan2(fz - centerZ, fx - centerX) + (random.nextFloat() - 0.5) * 0.9;
                fx += Math.cos(outward);
                fz += Math.sin(outward);
            }
        }
    }

    /**
     * Rejects spots a lake wouldn't sit in — the same intent as {@code LakeFeature}'s checks: the ground
     * under the pool and out to its rim must be solid, dry, and roughly level, or the pool ends up hanging
     * over a cliff or sitting in the middle of a water body.
     */
    private static boolean unsuitable(WorldGenLevel level, BlockPos origin, int radius, int depth) {
        int fromY = origin.getY() + 3;
        int toY = origin.getY() - depth - 6;
        int centerY = surfaceOf(level, origin.getX(), origin.getZ(), fromY, toY);
        if (centerY == Integer.MIN_VALUE || flooded(level, origin.getX(), centerY, origin.getZ())) {
            return true;
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int x = origin.getX() + (int) Math.round(Math.cos(a) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(a) * radius);
            int gy = surfaceOf(level, x, z, fromY, toY);
            if (gy == Integer.MIN_VALUE || Math.abs(gy - centerY) > 4 || flooded(level, x, gy, z)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the block just above the ground at this column is a fluid (i.e. the ground is underwater). */
    private static boolean flooded(WorldGenLevel level, int x, int groundY, int z) {
        return !level.getFluidState(new BlockPos(x, groundY + 1, z)).isEmpty();
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
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
