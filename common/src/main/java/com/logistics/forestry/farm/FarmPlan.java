package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotLayout;

/**
 * Which {@link FarmLogic} works each plot of a farm's ring, and the region it sees while doing so.
 * The {@link FarmComponent} takes every per-plot decision through {@link #logicFor}, never from a
 * logic it holds itself. Plot indices are the ring's.
 *
 * <p>Today every farm block runs one logic over its whole ring ({@link SingleLogicPlan}); the seam
 * keeps a farm that splits its ring between several logics possible.
 */
public interface FarmPlan {

    /** The logic that works plot {@code plotIndex}. */
    FarmLogic logicFor(int plotIndex);

    /** The region plot {@code plotIndex} belongs to, as {@link #logicFor} of it sees it. */
    PlotLayout regionFor(int plotIndex);
}
