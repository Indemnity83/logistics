package com.logistics.forestry.farm;

/**
 * A farm's per-tick work allowance: two token buckets and the turn between its two lanes.
 *
 * <p>The action bucket refills by {@code actionsPerSecond / 20} actions a tick, the check bucket by
 * {@code checksPerSecond / 20} checks, each check worth {@code readsPerCheck} block reads. Each holds
 * two ticks' worth, or at a rate below one a tick just under one more tick's worth than a whole one,
 * so that no refill is ever lost; on top of that no tick spends more than
 * {@link #actionCeiling}/{@link #readCeiling}, however long the farm sat idle. Buckets count in
 * twentieths, so whole-number rates refill exactly. Reads are charged as they are made; a charge
 * larger than the bucket leaves a debt that later refills pay off. Nothing here is saved.
 *
 * <p>When both lanes have work, {@link #turn} picks whose action is next so that the harvest lane
 * gets {@code harvestShare} of the actions; the turn only moves on an action taken while both had
 * work, so either lane takes the whole budget while the other has none.
 */
final class WorkBudget {

    static final int TICKS_PER_SECOND = 20;

    private static final int WHOLE_SHARE = 1000;

    enum Lane {
        HARVEST,
        CULTIVATE
    }

    private long actionUnits;
    private long readUnits;
    private long tickReads;
    private long tickReadLimit;
    private int turnCredit;

    /** Starts the action bucket part-filled by {@code seed}, so farms loaded together act on different ticks. */
    void stagger(long seed) {
        actionUnits = Math.floorMod(seed, TICKS_PER_SECOND);
    }

    /** Starts a tick: refills both buckets. */
    void refill(int actionsPerSecond, int checksPerSecond, int readsPerCheck) {
        actionUnits = Math.min(capacity(actionsPerSecond), actionUnits + actionsPerSecond);
        readUnits = Math.min(capacity(checksPerSecond) * readsPerCheck, readUnits + (long) checksPerSecond * readsPerCheck);
        tickReads = 0;
        tickReadLimit = readCeiling(checksPerSecond, readsPerCheck);
    }

    /** A bucket's size in twentieths at {@code perSecond}: two ticks' worth, but always room for a whole one plus a refill. */
    private static long capacity(int perSecond) {
        return Math.max(2L * perSecond, TICKS_PER_SECOND + perSecond - 1L);
    }

    /** The most actions any tick may take at {@code actionsPerSecond}. */
    static int actionCeiling(int actionsPerSecond) {
        return Math.max(1, (int) Math.ceil(2.0 * actionsPerSecond / TICKS_PER_SECOND));
    }

    /** The most block reads any tick may make at {@code checksPerSecond}. */
    static long readCeiling(int checksPerSecond, int readsPerCheck) {
        return Math.max(1, (long) Math.ceil(2.0 * checksPerSecond / TICKS_PER_SECOND)) * readsPerCheck;
    }

    boolean hasAction() {
        return actionUnits >= TICKS_PER_SECOND;
    }

    void spendAction() {
        actionUnits -= TICKS_PER_SECOND;
    }

    /** Whole block reads this tick may still make: what the check bucket holds, within the tick's ceiling. */
    long reads() {
        return Math.max(0, Math.min(readUnits / TICKS_PER_SECOND, tickReadLimit - tickReads));
    }

    void spendReads(long reads) {
        readUnits -= reads * TICKS_PER_SECOND;
        tickReads += reads;
    }

    /** The lane whose turn it is while both have work. */
    Lane turn(double harvestShare) {
        return turnCredit + permille(harvestShare) >= WHOLE_SHARE ? Lane.HARVEST : Lane.CULTIVATE;
    }

    /** Records an action {@code lane} took while both lanes had work. */
    void took(Lane lane, double harvestShare) {
        turnCredit += permille(harvestShare);
        if (lane == Lane.HARVEST) {
            turnCredit -= WHOLE_SHARE;
        }
        // A lane that lost its turn is owed at most one action.
        turnCredit = Math.clamp(turnCredit, -WHOLE_SHARE, WHOLE_SHARE);
    }

    private static int permille(double share) {
        return (int) Math.round(Math.clamp(share, 0.0, 1.0) * WHOLE_SHARE);
    }
}
