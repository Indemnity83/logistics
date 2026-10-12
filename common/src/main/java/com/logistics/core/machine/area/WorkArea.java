package com.logistics.core.machine.area;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Square, single-layer work area in front of a machine. The near row is the row directly in front
 * of {@code origin}, centred laterally on it, and the square extends {@code size} rows outward
 * along {@code facing}.
 *
 * <p>Plots are indexed rows first, from the near row outward; within a row, from left to right as
 * seen looking along {@code facing}. All positions are at {@code origin}'s Y; membership tests
 * ignore Y.
 */
public record WorkArea(BlockPos origin, Direction facing, int size) {

    public WorkArea {
        if (origin == null || facing == null) {
            throw new IllegalArgumentException("origin and facing are required");
        }
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("facing must be horizontal: " + facing);
        }
        if (size < 1 || size % 2 == 0) {
            throw new IllegalArgumentException("size must be odd and >= 1: " + size);
        }
        origin = origin.immutable();
    }

    public static WorkArea inFront(BlockPos origin, Direction facing, int size) {
        return new WorkArea(origin, facing, size);
    }

    /** Same origin and facing, grown by {@code bonus} blocks on every side. */
    public WorkArea withRangeBonus(int bonus) {
        if (bonus < 0) {
            throw new IllegalArgumentException("bonus must be >= 0: " + bonus);
        }
        return new WorkArea(origin, facing, size + 2 * bonus);
    }

    public int plotCount() {
        return size * size;
    }

    public BlockPos plot(int index) {
        if (index < 0 || index >= plotCount()) {
            throw new IndexOutOfBoundsException("plot index " + index + " outside 0.." + (plotCount() - 1));
        }
        int row = index / size;
        int column = index % size;
        int lateral = column - size / 2;
        Direction right = facing.getClockWise();
        return origin.relative(facing, row + 1).relative(right, lateral);
    }

    /** Plot index of {@code pos} (Y ignored), or -1 when it lies outside the area. */
    public int indexOf(BlockPos pos) {
        int dx = pos.getX() - origin.getX();
        int dz = pos.getZ() - origin.getZ();
        Direction right = facing.getClockWise();
        int row = dx * facing.getStepX() + dz * facing.getStepZ() - 1;
        int column = dx * right.getStepX() + dz * right.getStepZ() + size / 2;
        if (row < 0 || row >= size || column < 0 || column >= size) {
            return -1;
        }
        return row * size + column;
    }

    /** True when {@code pos} lies inside the area's columns, at any Y. */
    public boolean contains(BlockPos pos) {
        return indexOf(pos) >= 0;
    }

    /** Lowest-X, lowest-Z corner, at the origin's Y. */
    public BlockPos min() {
        BlockPos a = plot(0);
        BlockPos b = plot(plotCount() - 1);
        return new BlockPos(Math.min(a.getX(), b.getX()), origin.getY(), Math.min(a.getZ(), b.getZ()));
    }

    /** Highest-X, highest-Z corner, at the origin's Y. */
    public BlockPos max() {
        BlockPos a = plot(0);
        BlockPos b = plot(plotCount() - 1);
        return new BlockPos(Math.max(a.getX(), b.getX()), origin.getY(), Math.max(a.getZ(), b.getZ()));
    }
}
