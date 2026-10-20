package com.logistics.core.machine.area;

import net.minecraft.core.BlockPos;

/**
 * A worker's view of the region of plots it is responsible for, in region-relative coordinates.
 * Plots form lanes; a plot is addressed by its region index, or by lane and step within the lane.
 * Nothing here exposes the size of any larger area the region is part of.
 *
 * <p>All positions are at the region's Y; membership tests ignore Y.
 */
public interface PlotLayout {

    /** Whether {@code pos}'s column is one of the region's plots. */
    boolean contains(BlockPos pos);

    /** Region index of {@code pos} (Y ignored), or -1 when it is not one of the region's plots. */
    int indexOf(BlockPos pos);

    int laneCount();

    /** Plots in {@code lane}. */
    int laneLength(int lane);

    /** Lane holding the plot at region index {@code index}. */
    int lane(int index);

    /** Position of the plot at region index {@code index} within its lane. */
    int step(int index);

    /** The plot at {@code step} of {@code lane}. */
    BlockPos plot(int lane, int step);

    /** Smallest rectangle of columns holding every plot of the region. */
    Columns bounds();

    /** Columns that nothing working the region may ever touch, e.g. the courtyard a ring surrounds. */
    default Columns keepOut() {
        return Columns.EMPTY;
    }
}
