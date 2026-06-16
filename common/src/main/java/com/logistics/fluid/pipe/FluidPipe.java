package com.logistics.fluid.pipe;

/**
 * Pure model of a single fluid pipe segment's internal storage.
 *
 * <p>Holds no Minecraft types and works entirely in millibuckets (mB), so it is unit-testable without a
 * running game. The block-entity adapter converts mB to platform-native fluid units at the world boundary.
 * Fluid identity is the opaque type {@code F} (e.g. a fluid key); the pipe only compares it for equality and
 * never mixes two different fluids.
 *
 * <p>The pipe carries no fuel or rate of its own. Each tick the world adapter passes in how much it may move
 * (its <em>allowance</em>, derived from config transfer rate and stored energy) and
 * {@link #extract(FluidProvider, long)} pulls up to that, bounded by remaining space and what the provider
 * holds.
 *
 * @param <F> fluid identity type
 */
public final class FluidPipe<F> {

    /** Internal storage capacity of a pipe segment, in millibuckets. */
    public static final long CAPACITY_MB = 250;

    /** How much fluid is currently held in this pipe's internal storage, in millibuckets. */
    private long amountMb;

    /** The fluid currently held, or {@code null} when the pipe is empty. */
    private F fluid;

    /** Creates an extraction pipe — one that can pull fluid from adjacent storage. */
    public static <F> FluidPipe<F> extractor() {
        return new FluidPipe<>();
    }

    /** Capacity of this pipe's internal storage, in millibuckets. */
    public long capacity() {
        return CAPACITY_MB;
    }

    /** Amount of fluid currently held in this pipe's internal storage, in millibuckets. */
    public long amount() {
        return amountMb;
    }

    /** The fluid currently held, or {@code null} when the pipe is empty. */
    public F fluid() {
        return fluid;
    }

    /**
     * Restores persisted state — the held fluid and stored amount. The world adapter calls this each tick to
     * rebuild a transient pipe from its block entity before extracting; it bypasses the extraction rules, so
     * callers must pass an already-valid amount.
     */
    public void restore(F fluid, long amountMb) {
        this.amountMb = Math.max(0, Math.min(amountMb, CAPACITY_MB));
        this.fluid = this.amountMb > 0 ? fluid : null;
    }

    /**
     * Extracts up to {@code millibuckets} of fluid from {@code source} into internal storage. Capped by the
     * remaining space and what the provider actually holds — fluid removed from the provider always equals
     * fluid inserted here. Nothing is transferred if the provider is empty or offers a fluid different from
     * what this pipe already holds.
     *
     * @return the amount actually extracted, in millibuckets
     */
    public long extract(FluidProvider<F> source, long millibuckets) {
        if (millibuckets <= 0) {
            return 0;
        }
        F incoming = source.fluid();
        if (incoming == null || (fluid != null && !fluid.equals(incoming))) {
            return 0;
        }
        long space = CAPACITY_MB - amountMb;
        long limit = Math.min(millibuckets, space);
        long extracted = source.drain(limit);
        if (extracted > 0) {
            fluid = incoming;
            amountMb += extracted;
        }
        return extracted;
    }

    /**
     * Pushes up to {@code millibuckets} of fluid from this pipe into {@code target}, equalizing their levels:
     * it moves at most half the difference, so a transfer never leaves the target fuller than this pipe
     * (communicating-vessels behaviour). Nothing moves if this pipe is empty, the target is already at least
     * as full, or the target holds a different fluid.
     *
     * @return the amount actually moved, in millibuckets
     */
    public long push(FluidPipe<F> target, long millibuckets) {
        if (!canTransferTo(target, millibuckets)) {
            return 0;
        }
        // Round the half up so an odd 1 mB gap still moves — otherwise levels stall 1 apart and stair-step,
        // and pipes never reach full (which the head lift requires). The target ends at most 1 mB fuller.
        return transfer(target, Math.min(millibuckets, (amountMb - target.amountMb + 1) / 2));
    }

    /**
     * Pours up to {@code millibuckets} of fluid from this pipe into {@code target} greedily — unlike
     * {@link #push}, it is not capped at half the difference, so the target can end up fuller than this pipe.
     * Used for the downward (gravity) direction. Still bounded by what this pipe holds and the target's space,
     * and refuses to mix fluids.
     */
    public long pour(FluidPipe<F> target, long millibuckets) {
        if (!canTransferTo(target, millibuckets)) {
            return 0;
        }
        long space = CAPACITY_MB - target.amountMb;
        return transfer(target, Math.min(Math.min(millibuckets, amountMb), space));
    }

    /**
     * Lifts up to {@code millibuckets} of fluid from this pipe into {@code target} above it, greedily (like
     * {@link #pour}). Only happens when this pipe is <em>full</em> and {@code head} — the remaining lift
     * capacity in blocks — is positive: fluid pools and fills a pipe first, then climbs under pressure.
     */
    public long lift(FluidPipe<F> target, long millibuckets, int head) {
        if (head <= 0 || amountMb < CAPACITY_MB) {
            return 0;
        }
        return pour(target, millibuckets);
    }

    /** True if this pipe can send {@code millibuckets} into {@code target}: it has fluid and they match. */
    private boolean canTransferTo(FluidPipe<F> target, long millibuckets) {
        return fluid != null && millibuckets > 0 && (target.fluid == null || target.fluid.equals(fluid));
    }

    /** Applies a transfer of {@code amount} mB into {@code target} (no-op if {@code amount <= 0}). */
    private long transfer(FluidPipe<F> target, long amount) {
        if (amount <= 0) {
            return 0;
        }
        target.fluid = fluid;
        target.amountMb += amount;
        amountMb -= amount;
        if (amountMb == 0) {
            fluid = null;
        }
        return amount;
    }
}
