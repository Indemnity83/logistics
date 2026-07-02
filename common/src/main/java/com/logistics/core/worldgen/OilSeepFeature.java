package com.logistics.core.worldgen;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A surface crude oil "seep": an organic bowl of crude oil whose basin is lined with the biome's oil ore
 * (oil sand in deserts, oil red sand in badlands, oil shale elsewhere). Air-adjacent faces are always
 * lined so the pool stays contained; each solid face becomes ore only at the config's {@code shellChance}
 * so the banks look oil-soaked rather than fully cased. Rejects cliff/water spots the way a lake would.
 */
public class OilSeepFeature extends Feature<OilSeepConfiguration> {

    private static final Direction[] WALL_DIRS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final TagKey<Biome> IS_DESERT =
        TagKey.create(Registries.BIOME, ResourceId.in("c", "is_desert").toIdentifier());
    private static final TagKey<Biome> IS_BADLANDS =
        TagKey.create(Registries.BIOME, ResourceId.in("c", "is_badlands").toIdentifier());

    public OilSeepFeature(Codec<OilSeepConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSeepConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilSeepConfiguration config = context.config();
        BlockState fluid = config.fluid();
        int radius = Math.max(2, config.radius() + random.nextInt(3) - 1);
        int depth = Math.max(1, config.depth() + random.nextInt(3) - 1);

        BlockPos origin = context.origin();
        if (unsuitable(level, origin, radius, depth)) {
            return false;
        }
        BlockState shell = shellFor(level, origin);
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

        // Line the basin: air/fluid faces are always sealed (containment); solid faces turn to ore only
        // at shellChance, leaving patches of the natural ground showing through.
        for (long packed : oil) {
            BlockPos oilPos = BlockPos.of(packed);
            for (Direction dir : WALL_DIRS) {
                BlockPos wall = oilPos.relative(dir);
                if (oil.contains(wall.asLong())) {
                    continue;
                }
                boolean leaky = level.isEmptyBlock(wall) || !level.getFluidState(wall).isEmpty();
                if (leaky || random.nextFloat() < config.shellChance()) {
                    level.setBlock(wall, shell, 2);
                }
            }
        }
        return true;
    }

    /** The oil ore for this biome: oil sand in deserts, oil red sand in badlands, oil shale elsewhere. */
    private static BlockState shellFor(WorldGenLevel level, BlockPos origin) {
        var biome = level.getBiome(origin);
        if (biome.is(IS_DESERT)) {
            return LogisticsCore.BLOCK.OIL_SAND.defaultBlockState();
        }
        if (biome.is(IS_BADLANDS)) {
            return LogisticsCore.BLOCK.OIL_RED_SAND.defaultBlockState();
        }
        return LogisticsCore.BLOCK.OIL_SHALE.defaultBlockState();
    }

    /**
     * Rejects spots a lake wouldn't sit in: the ground under the pool and out to its rim must be solid,
     * dry, and roughly level, or the pool hangs over a cliff or sits in a water body.
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
