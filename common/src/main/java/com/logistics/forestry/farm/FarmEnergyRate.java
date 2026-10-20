package com.logistics.forestry.farm;

/**
 * A farm's RF/t averaged over the last {@link #WINDOW} ticks. A farm spends RF in single actions a
 * few ticks apart, so its per-tick spend alternates between 0 and an action's cost; the average reads
 * steadily. Fed one value per tick; not saved.
 */
final class FarmEnergyRate {

    static final int WINDOW = 20;

    private final long[] ticks = new long[WINDOW];
    private int next;
    private long sum;

    /** Records the RF spent in one tick, dropping the oldest tick from the window. */
    void record(long rf) {
        sum += rf - ticks[next];
        ticks[next] = rf;
        next = (next + 1) % WINDOW;
    }

    /** The average RF/t over the window, rounded; ticks not yet recorded count as 0. */
    long perTick() {
        return Math.round(sum / (double) WINDOW);
    }
}
