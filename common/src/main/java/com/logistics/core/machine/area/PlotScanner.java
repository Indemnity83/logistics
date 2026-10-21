package com.logistics.core.machine.area;

/**
 * Round-robin cursor over a fixed number of positions with exponential idle back-off.
 *
 * <p>A pass is {@code positionCount} consecutive {@link #next()} calls. When a pass ends without any
 * {@link #markWorked()}, {@link #idleTicks()} becomes the back-off to wait before continuing: the
 * base delay after the first empty pass, doubling per further consecutive empty pass, capped at the
 * maximum. Any {@link #markWorked()} resets it to 0.
 *
 * <p>A farm scans its Reach with this: every position of the Reach, Plot or not, is one position.
 *
 * <p>Only the cursor is meant to be persisted; back-off state restarts from zero on load.
 * Not thread-safe.
 */
public final class PlotScanner {

    private final int baseIdleTicks;
    private final int maxIdleTicks;
    private int positionCount;
    private int cursor;
    private int stepsInPass;
    private boolean workedThisPass;
    private int idleTicks;

    public PlotScanner(int positionCount, int baseIdleTicks, int maxIdleTicks) {
        if (baseIdleTicks < 1 || maxIdleTicks < baseIdleTicks) {
            throw new IllegalArgumentException(
                    "need 1 <= baseIdleTicks <= maxIdleTicks: " + baseIdleTicks + ", " + maxIdleTicks);
        }
        this.baseIdleTicks = baseIdleTicks;
        this.maxIdleTicks = maxIdleTicks;
        resize(positionCount);
    }

    /** Rebuilds a scanner from a saved cursor; an out-of-range cursor restarts at 0. */
    public static PlotScanner restore(int positionCount, int cursor, int baseIdleTicks, int maxIdleTicks) {
        PlotScanner scanner = new PlotScanner(positionCount, baseIdleTicks, maxIdleTicks);
        if (cursor >= 0 && cursor < positionCount) {
            scanner.cursor = cursor;
        }
        return scanner;
    }

    /** Returns the current position index and advances, wrapping at {@code positionCount}. */
    public int next() {
        int index = cursor;
        cursor = (cursor + 1) % positionCount;
        stepsInPass++;
        if (stepsInPass >= positionCount) {
            if (!workedThisPass) {
                idleTicks = idleTicks == 0 ? baseIdleTicks : (int) Math.min((long) idleTicks * 2, maxIdleTicks);
            }
            stepsInPass = 0;
            workedThisPass = false;
        }
        return index;
    }

    /** Records that the position last returned by {@link #next()} was worked. */
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

    /** Changes the position count; keeps the cursor if still in range, otherwise restarts at 0. */
    public void resize(int newPositionCount) {
        if (newPositionCount < 1) {
            throw new IllegalArgumentException("positionCount must be >= 1: " + newPositionCount);
        }
        positionCount = newPositionCount;
        if (cursor >= positionCount) {
            cursor = 0;
        }
        stepsInPass = 0;
        workedThisPass = false;
        idleTicks = 0;
    }

    public int positionCount() {
        return positionCount;
    }

    public int cursor() {
        return cursor;
    }
}
