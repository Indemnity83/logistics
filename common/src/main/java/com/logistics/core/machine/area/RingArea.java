package com.logistics.core.machine.area;

import net.minecraft.core.BlockPos;

/**
 * Square, single-layer ring centred on {@code origin}: every column of the {@code outer} square
 * that lies outside the {@code inner} square. Both sides are odd, so both squares are centred
 * exactly on {@code origin}, which is never a plot.
 *
 * <p>The ring is made of concentric lanes. Lane 0 is the perimeter of the square of side {@code
 * inner + 2}, and each further lane is the perimeter of the square two blocks wider. Plots are
 * indexed lane by lane, from the innermost outward. Within a lane, step 0 is the north-west corner
 * (lowest X, lowest Z) and steps run clockwise seen from above: east along the north edge, south
 * along the east edge, west along the south edge, then north along the west edge. Consecutive
 * steps are orthogonally adjacent, and the last step is adjacent to step 0.
 *
 * <p>All positions are at {@code origin}'s Y; membership tests ignore Y.
 */
public record RingArea(BlockPos origin, int inner, int outer) implements PlotArea {

    public RingArea {
        if (origin == null) {
            throw new IllegalArgumentException("origin is required");
        }
        if (inner < 1 || inner % 2 == 0) {
            throw new IllegalArgumentException("inner must be odd and >= 1: " + inner);
        }
        if (outer <= inner || outer % 2 == 0) {
            throw new IllegalArgumentException("outer must be odd and > inner (" + inner + "): " + outer);
        }
        origin = origin.immutable();
    }

    @Override
    public int plotCount() {
        return outer * outer - inner * inner;
    }

    public int laneCount() {
        return (outer - inner) / 2;
    }

    /** Plots in {@code lane}: the perimeter of the square of side {@code inner + 2 + 2 * lane}. */
    public int laneLength(int lane) {
        checkLane(lane);
        return 8 * halfSide(lane);
    }

    /** Lane holding plot {@code index}. */
    public int lane(int index) {
        checkIndex(index);
        int lane = 0;
        while (index >= laneStart(lane + 1)) {
            lane++;
        }
        return lane;
    }

    /** Position of plot {@code index} within its lane. */
    public int step(int index) {
        return index - laneStart(lane(index));
    }

    /** Plot index of {@code step} within {@code lane}. */
    public int index(int lane, int step) {
        checkStep(lane, step);
        return laneStart(lane) + step;
    }

    @Override
    public BlockPos plot(int index) {
        int lane = lane(index);
        return plot(lane, index - laneStart(lane));
    }

    public BlockPos plot(int lane, int step) {
        checkStep(lane, step);
        int h = halfSide(lane);
        int edge = step / (2 * h);
        int t = step % (2 * h);
        return switch (edge) {
            case 0 -> origin.offset(-h + t, 0, -h);
            case 1 -> origin.offset(h, 0, -h + t);
            case 2 -> origin.offset(h - t, 0, h);
            default -> origin.offset(-h, 0, h - t);
        };
    }

    @Override
    public int indexOf(BlockPos pos) {
        int dx = pos.getX() - origin.getX();
        int dz = pos.getZ() - origin.getZ();
        int h = Math.max(Math.abs(dx), Math.abs(dz));
        if (h <= inner / 2 || h > outer / 2) {
            return -1;
        }
        int step;
        if (dz == -h && dx < h) {
            step = dx + h;
        } else if (dx == h && dz < h) {
            step = 3 * h + dz;
        } else if (dz == h && dx > -h) {
            step = 5 * h - dx;
        } else {
            step = 7 * h - dz;
        }
        return laneStart(h - inner / 2 - 1) + step;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return indexOf(pos) >= 0;
    }

    @Override
    public BlockPos min() {
        return origin.offset(-(outer / 2), 0, -(outer / 2));
    }

    @Override
    public BlockPos max() {
        return origin.offset(outer / 2, 0, outer / 2);
    }

    /** Distance from {@code origin} to {@code lane}'s edges. */
    private int halfSide(int lane) {
        return inner / 2 + 1 + lane;
    }

    /** Index of {@code lane}'s step 0: the plots of every lane inside it. */
    private int laneStart(int lane) {
        int side = inner + 2 * lane;
        return side * side - inner * inner;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= plotCount()) {
            throw new IndexOutOfBoundsException("plot index " + index + " outside 0.." + (plotCount() - 1));
        }
    }

    private void checkLane(int lane) {
        if (lane < 0 || lane >= laneCount()) {
            throw new IndexOutOfBoundsException("lane " + lane + " outside 0.." + (laneCount() - 1));
        }
    }

    private void checkStep(int lane, int step) {
        int length = laneLength(lane);
        if (step < 0 || step >= length) {
            throw new IndexOutOfBoundsException("step " + step + " outside 0.." + (length - 1));
        }
    }
}
