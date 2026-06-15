package com.logistics.fluid.tank;

import com.logistics.core.lib.fluids.IFluidKey;

/**
 * One tank in a vertical column, abstracted over the block entity so {@link TankColumn} can read and
 * rewrite its contents without touching world/loader types. The tank block entity implements this over
 * its {@link com.logistics.core.lib.fluids.FluidTankComponent}. Amounts are platform-native units.
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
}
