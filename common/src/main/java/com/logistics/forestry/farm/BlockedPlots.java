package com.logistics.forestry.farm;

import java.util.BitSet;

/**
 * Which plots of the ring came back blocked (obstructed, too dark or protected) on their most
 * recent visit. Every plot is visited once a pass, so this is the last full pass's count; before the
 * first pass completes it counts what has been visited so far. Not persisted.
 */
final class BlockedPlots {

    private final BitSet blocked = new BitSet();
    private int plotCount;

    /**
     * Follows the ring to {@code plotCount} plots. A wider tier only adds lanes and keeps every
     * existing plot's index, so known plots are kept; plots past the new end are dropped.
     */
    void resize(int plotCount) {
        if (plotCount < this.plotCount) {
            blocked.clear(plotCount, this.plotCount);
        }
        this.plotCount = plotCount;
    }

    /** Records the outcome of visiting plot {@code index}. */
    void record(int index, FarmStatus outcome) {
        if (index < 0 || index >= plotCount) {
            return;
        }
        blocked.set(index, isBlocked(outcome));
    }

    int count() {
        return blocked.cardinality();
    }

    static boolean isBlocked(FarmStatus outcome) {
        return outcome == FarmStatus.BLOCKED || outcome == FarmStatus.TOO_DARK;
    }
}
