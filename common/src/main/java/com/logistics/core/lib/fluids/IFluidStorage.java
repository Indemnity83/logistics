package com.logistics.core.lib.fluids;

/**
 * Loader-agnostic fluid storage contract.
 *
 * <p>Uses simulate-boolean semantics (not transactions) to remain compatible with both
 * Fabric (Transfer API) and NeoForge fluid APIs. Loader-specific adapters bridge this
 * interface to each platform's native API.
 *
 * <p>This is the primary extension point for the fluid-storage abstraction layer. Any block
 * entity that participates in fluid transfer should implement
 * {@link com.logistics.core.lib.block.capability.HasFluidStorage} and return this type.
 *
 * @see com.logistics.core.lib.block.capability.HasFluidStorage
 * @see FluidStorageLookup
 */
public interface IFluidStorage {

    /**
     * Insert fluid into this storage.
     *
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} immediately.
     * The return value is always {@code >= 0} and never exceeds {@code maxAmount}.
     *
     * @param fluid     the fluid type to insert
     * @param maxAmount the maximum amount to insert, in platform-native units
     * @param simulate  if {@code true}, perform a dry run without modifying state; the returned
     *                  value is the amount that would be inserted without any state change
     * @return the amount actually inserted (or that would be inserted if simulating); never negative
     */
    long insert(IFluidKey fluid, long maxAmount, boolean simulate);

    /**
     * Extract fluid from this storage.
     *
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} immediately.
     * The return value is always {@code >= 0} and never exceeds {@code maxAmount}.
     *
     * @param fluid     the fluid type to extract
     * @param maxAmount the maximum amount to extract, in platform-native units
     * @param simulate  if {@code true}, perform a dry run without modifying state; the returned
     *                  value is the amount that would be extracted without any state change
     * @return the amount actually extracted (or that would be extracted if simulating); never negative
     */
    long extract(IFluidKey fluid, long maxAmount, boolean simulate);

    /**
     * Iterate over the contents of this storage.
     *
     * <p>Each {@link IFluidView} represents a non-empty, non-blank resource.
     * Implementations must omit empty and blank entries — callers may rely on this.
     *
     * @return an iterable over the non-empty views in this storage
     */
    Iterable<IFluidView> contents();
}
