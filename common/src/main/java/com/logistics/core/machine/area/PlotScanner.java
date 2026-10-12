package com.logistics.core.machine.area;

/**
 * Round-robin cursor over a fixed number of plots with exponential idle back-off.
 *
 * <p>A pass is {@code plotCount} consecutive {@link #next()} calls. When a pass ends without any
 * {@link #markWorked()}, {@link #idleTicks()} becomes the back-off to wait before continuing: the
 * base delay after the first empty pass, doubling per further consecutive empty pass, capped at the
 * maximum. Any {@link #markWorked()} resets it to 0.
 *
 * <p>Only the cursor is meant to be persisted; back-off state restarts from zero on load.
 * Not thread-safe.
 */
public final class PlotScanner {

    private final int baseIdleTicks;
    private final int maxIdleTicks;
    private int plotCount;
    private int cursor;
    private int stepsInPass;
    private boolean workedThisPass;
    private int idleTicks;

    public PlotScanner(int plotCount, int baseIdleTicks, int maxIdleTicks) {
        if (baseIdleTicks < 1 || maxIdleTicks < baseIdleTicks) {
            throw new IllegalArgumentException(
                    "need 1 <= baseIdleTicks <= maxIdleTicks: " + baseIdleTicks + ", " + maxIdleTicks);
        }
        this.baseIdleTicks = baseIdleTicks;
        this.maxIdleTicks = maxIdleTicks;
        resize(plotCount);
    }

    /** Rebuilds a scanner from a saved cursor; an out-of-range cursor restarts at 0. */
    public static PlotScanner restore(int plotCount, int cursor, int baseIdleTicks, int maxIdleTicks) {
        PlotScanner scanner = new PlotScanner(plotCount, baseIdleTicks, maxIdleTicks);
        if (cursor >= 0 && cursor < plotCount) {
            scanner.cursor = cursor;
        }
        return scanner;
    }

    /** Returns the current plot index and advances, wrapping at {@code plotCount}. */
    public int next() {
        int index = cursor;
        cursor = (cursor + 1) % plotCount;
        stepsInPass++;
        if (stepsInPass >= plotCount) {
            if (!workedThisPass) {
                idleTicks = idleTicks == 0 ? baseIdleTicks : (int) Math.min((long) idleTicks * 2, maxIdleTicks);
            }
            stepsInPass = 0;
            workedThisPass = false;
        }
        return index;
    }

    /** Records that the plot last returned by {@link #next()} was worked. */
    public void markWorked() {
        idleTicks = 0;
        // Right after a pass closes, the work belongs to the pass that just ended, not the new one.
        if (stepsInPass > 0) {
            workedThisPass = true;
        }
    }

    /** Back-off after the last completed pass; 0 while the scanner is finding work. */
    public int idleTicks() {
        return idleTicks;
    }

    /** Changes the plot count; keeps the cursor if still in range, otherwise restarts at 0. */
    public void resize(int newPlotCount) {
        if (newPlotCount < 1) {
            throw new IllegalArgumentException("plotCount must be >= 1: " + newPlotCount);
        }
        plotCount = newPlotCount;
        if (cursor >= plotCount) {
            cursor = 0;
        }
        stepsInPass = 0;
        workedThisPass = false;
        idleTicks = 0;
    }

    public int plotCount() {
        return plotCount;
    }

    public int cursor() {
        return cursor;
    }
}
