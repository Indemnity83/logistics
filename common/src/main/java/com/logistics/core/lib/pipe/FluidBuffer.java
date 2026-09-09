package com.logistics.core.lib.pipe;

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
public final class FluidBuffer<F> {

    /** Default internal storage capacity of a pipe segment, in millibuckets. */
    public static final long CAPACITY_MB = 250;

    /** This buffer's storage capacity, in millibuckets. */
    private final long capacityMb;

    /** How much fluid is currently held in this pipe's internal storage, in millibuckets. */
    private long amountMb;

    /** The fluid currently held, or {@code null} when the pipe is empty. */
    private F fluid;

    public FluidBuffer() {
        this(CAPACITY_MB);
    }

    public FluidBuffer(long capacityMb) {
        this.capacityMb = Math.max(0, capacityMb);
    }

    /** Creates an extraction pipe — one that can pull fluid from adjacent storage. */
    public static <F> FluidBuffer<F> extractor() {
        return new FluidBuffer<>();
    }

    /** Creates an extraction pipe holding up to {@code capacityMb} millibuckets. */
    public static <F> FluidBuffer<F> extractor(long capacityMb) {
        return new FluidBuffer<>(capacityMb);
    }

    /** Capacity of this pipe's internal storage, in millibuckets. */
    public long capacity() {
        return capacityMb;
    }

    /** Remaining space in this pipe's internal storage, in millibuckets. */
    public long space() {
        return capacityMb - amountMb;
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
        long limit = Math.min(millibuckets, space());
        long extracted = source.drain(limit);
        if (extracted > 0) {
            fluid = incoming;
            amountMb += extracted;
        }
        return extracted;
    }
}
