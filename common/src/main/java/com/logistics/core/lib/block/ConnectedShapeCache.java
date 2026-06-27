package com.logistics.core.lib.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Precomputes the 64 collision shapes a connected block can take — a core shape unioned with any
 * subset of six directional arms — so {@code getShape} is an array lookup instead of rebuilding the
 * union with {@link Shapes#or} on every call (which collision and ray-tracing hit frequently).
 *
 * <p>Arms are indexed by {@link Direction#get3DDataValue()}, and the cache key is a 6-bit mask of
 * the connected directions. Connection state is still resolved live by the caller; only the shape
 * union is cached.
 */
public final class ConnectedShapeCache {
    private final VoxelShape[] byMask = new VoxelShape[64];

    /**
     * @param core the always-present center shape
     * @param arms six arm shapes indexed by {@link Direction#get3DDataValue()}
     */
    public ConnectedShapeCache(VoxelShape core, VoxelShape[] arms) {
        for (int mask = 0; mask < byMask.length; mask++) {
            VoxelShape shape = core;
            for (Direction dir : Direction.values()) {
                if ((mask & bit(dir)) != 0) {
                    shape = Shapes.or(shape, arms[dir.get3DDataValue()]);
                }
            }
            // Simplify once up front so cached collision/raycast lookups use the fewest boxes.
            byMask[mask] = shape.optimize();
        }
    }

    /** Bit for a direction within a connection mask. */
    public static int bit(Direction dir) {
        return 1 << dir.get3DDataValue();
    }

    /** @param connectionMask OR of {@link #bit(Direction)} for each connected direction */
    public VoxelShape get(int connectionMask) {
        return byMask[connectionMask];
    }
}
