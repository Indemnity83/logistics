package com.logistics.core.lib.fluids;

/**
 * Read-only view of a fluid resource within an {@link IFluidStorage}.
 *
 * <p>Equivalent to Fabric's {@code StorageView<FluidVariant>} in the loader-agnostic API.
 * Instances are obtained by iterating {@link IFluidStorage#contents()}.
 *
 * <p>Contract: {@link #resource()} never returns {@code null} and {@link #amount()} is always
 * {@code > 0} for any view produced by {@link IFluidStorage#contents()}. Implementations must
 * not produce zero-amount or blank views; callers may rely on this invariant.
 */
public interface IFluidView {

    /** The fluid type stored in this view. Never {@code null} or blank. */
    IFluidKey resource();

    /** The amount of fluid available in this view, in platform-native units. Always {@code > 0}. */
    long amount();
}
