package com.logistics.core.machine.area;

import net.minecraft.core.BlockPos;

/**
 * An axis-aligned rectangle of block columns, bounds inclusive; membership ignores Y. A rectangle
 * whose max lies below its min on either axis is empty.
 */
public record Columns(int minX, int minZ, int maxX, int maxZ) {

    /** The square of odd side {@code side} centred on {@code centre}'s column. */
    public static Columns square(BlockPos centre, int side) {
        if (side < 1 || side % 2 == 0) {
            throw new IllegalArgumentException("side must be odd and >= 1: " + side);
        }
        int half = side / 2;
        return new Columns(centre.getX() - half, centre.getZ() - half, centre.getX() + half, centre.getZ() + half);
    }

    public boolean isEmpty() {
        return maxX < minX || maxZ < minZ;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    /** This rectangle widened by {@code by} columns on every side; an empty one stays empty. */
    public Columns grow(int by) {
        return isEmpty() ? this : new Columns(minX - by, minZ - by, maxX + by, maxZ + by);
    }
}
