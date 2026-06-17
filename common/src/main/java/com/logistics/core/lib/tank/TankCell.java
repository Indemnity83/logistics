package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;

/**
 * One tank in a vertical column, abstracted over the block entity so {@link TankColumn} can read and
 * rewrite its contents without touching world/loader types. The tank block entity implements this over
 * its {@link com.logistics.core.lib.fluids.FluidTankComponent}. Amounts are platform-native units.
 *
 * <p>This is the public cross-mod contract: another mod's tank participates in a shared vertical column
 * by exposing a {@code TankCell} through {@link TankCellLookup}. {@link TankColumns} walks the stack via
 * that lookup, so columns can span block entities from different mods.
 */
public interface TankCell {

    /** The fluid currently held, or a blank key if empty. */
    IFluidKey fluid();

    /** Current contents, in native units. */
    long amount();

    /** Capacity, in native units. */
    long capacity();

    /** Raw write of {@code fluid} + {@code amount}; an amount of 0 empties the cell. */
    void setContents(IFluidKey fluid, long amount);

    /**
     * Whether this cell joins its vertical neighbours into a shared fluid column. Isolated tanks
     * (creative/void-style) return {@code false} so they form a single-cell column and never merge a
     * neighbour's fluid. Defaults to {@code true}.
     */
    default boolean joinsColumn() {
        return true;
    }
}
