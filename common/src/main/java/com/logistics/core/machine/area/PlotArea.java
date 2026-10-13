package com.logistics.core.machine.area;

import net.minecraft.core.BlockPos;

/**
 * Single-layer set of block columns, each addressed by a plot index in {@code 0..plotCount()-1}.
 * All positions are at the area's origin Y; membership tests ignore Y.
 */
public interface PlotArea {

    int plotCount();

    BlockPos plot(int index);

    /** Plot index of {@code pos} (Y ignored), or -1 when it lies outside the area. */
    int indexOf(BlockPos pos);

    /** True when {@code pos} lies inside the area's columns, at any Y. */
    boolean contains(BlockPos pos);

    /** Lowest-X, lowest-Z corner of the area's bounds, at the origin's Y. */
    BlockPos min();

    /** Highest-X, highest-Z corner of the area's bounds, at the origin's Y. */
    BlockPos max();
}
