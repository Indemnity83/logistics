package com.logistics.forestry.farm;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Turns per-plot outcomes into a steady farm status. A farm visits one plot per step, and the plots
 * differ, so the status reflects the last full pass rather than the last plot: a problem shows
 * while it was met within the last {@code window} steps and still applies, ahead of
 * {@link FarmStatus#WORKING}, which shows while any op succeeded within the window. With neither,
 * the farm is {@link FarmStatus#IDLE}.
 *
 * <p>Problems rank in a fixed order so the reported status does not flicker between them.
 */
final class FarmStatusTracker {

    /** Most important first. */
    private static final FarmStatus[] PROBLEMS = {
        FarmStatus.NO_POWER,
        FarmStatus.OUTPUT_FULL,
        FarmStatus.INVENTORY_FULL,
        FarmStatus.NO_SOIL,
        FarmStatus.NO_GERMLINGS,
        FarmStatus.WAITING_FOR_SEEDS,
        FarmStatus.TOO_DARK,
        FarmStatus.BLOCKED
    };

    private static final long NEVER = Long.MIN_VALUE / 2;

    private final long[] lastSeen = new long[FarmStatus.values().length];
    private long step;
    private long lastWorked = NEVER;

    FarmStatusTracker() {
        Arrays.fill(lastSeen, NEVER);
    }

    /** Starts the next plot step. */
    void step() {
        step++;
    }

    /** Records {@code outcome}: {@link FarmStatus#WORKING}, {@link FarmStatus#IDLE}, or a problem. */
    void record(FarmStatus outcome) {
        switch (outcome) {
            case WORKING -> {
                lastWorked = step;
                // Whatever just ran had the power it needed.
                clear(FarmStatus.NO_POWER);
            }
            case IDLE, PAUSED -> {}
            default -> lastSeen[outcome.ordinal()] = step;
        }
    }

    /** Forgets {@code problem}, e.g. once the op it blocked has succeeded. */
    void clear(FarmStatus problem) {
        lastSeen[problem.ordinal()] = NEVER;
    }

    FarmStatus status(int window, Predicate<FarmStatus> stillApplies) {
        for (FarmStatus problem : PROBLEMS) {
            if (step - lastSeen[problem.ordinal()] < window) {
                if (stillApplies.test(problem)) {
                    return problem;
                }
                clear(problem);
            }
        }
        return step - lastWorked < window ? FarmStatus.WORKING : FarmStatus.IDLE;
    }
}
