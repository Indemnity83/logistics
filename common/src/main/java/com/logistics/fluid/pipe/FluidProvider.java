package com.logistics.fluid.pipe;

/**
 * A source of fluid an extraction pipe can pull from — typically an adjacent tank.
 *
 * <p>Pure abstraction in millibuckets, with no Minecraft types, so pipe extraction is unit-testable.
 * The world adapter wraps a real fluid handler behind this interface. Fluid identity is the opaque
 * type {@code F} (e.g. a fluid key); the pipe only compares it for equality.
 *
 * @param <F> fluid identity type
 */
public interface FluidProvider<F> {

    /** The fluid this provider currently offers, or {@code null} if it holds nothing. */
    F fluid();

    /**
     * Removes up to {@code millibuckets} of fluid from this provider.
     *
     * @return the amount actually drained, in millibuckets (may be less if the provider holds less)
     */
    long drain(long millibuckets);
}
