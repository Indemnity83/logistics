package com.logistics.pipe.runtime;

import com.logistics.core.lib.pipe.RoutePlan;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

final class PipeRoutingPlanner {
    private PipeRoutingPlanner() {}

    static RoutePlan defaultPlan(List<Direction> validDirections) {
        return validDirections.isEmpty() ? RoutePlan.drop() : RoutePlan.reroute(validDirections);
    }

    static RoutePlan normalize(RoutePlan customPlan, RoutePlan defaultPlan) {
        RoutePlan normalized = switch (customPlan.getType()) {
            case REROUTE -> customPlan.getDirections().isEmpty() ? RoutePlan.drop() : customPlan;
            case SPLIT -> customPlan.getItems().isEmpty() ? RoutePlan.discard() : customPlan;
            default -> customPlan;
        };

        return normalized.getType() == RoutePlan.Type.PASS ? defaultPlan : normalized;
    }

    static Direction chooseDirection(BlockPos pos, long gameTime, Direction currentDirection, List<Direction> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }
        long seed = mixHash(pos.asLong(), gameTime, currentDirection.get3DDataValue());
        Random random = new Random(seed);
        return options.get(random.nextInt(options.size()));
    }

    private static long mixHash(long a, long b, long c) {
        long hash = a;
        hash = hash * 31 + b;
        hash = hash * 31 + c;

        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);

        return hash;
    }
}
