package com.logistics.fluid;

import com.logistics.core.lib.platform.PlatformService;

/**
 * Converts author-facing millibucket (mB) amounts into platform-native fluid units.
 *
 * <p>Fluid storage amounts are platform-native (Fabric droplets = 81/mB, NeoForge mB = 1/mB),
 * so capacities and transfer rates expressed in mB in config must be converted at the world
 * boundary before being handed to {@link com.logistics.core.lib.fluids.IFluidStorage}.
 *
 * <p>Not used in unit tests — tests operate directly in native units to avoid depending on a
 * loader {@link PlatformService}.
 */
public final class FluidUnits {

    private FluidUnits() {}

    /** Converts a millibucket amount to platform-native fluid units. */
    public static long mb(int millibuckets) {
        return (long) millibuckets * PlatformService.INSTANCE.fluidUnitsPerMillibucket();
    }

    /** Converts platform-native fluid units back to millibuckets, rounding down. */
    public static long toMillibuckets(long nativeUnits) {
        return nativeUnits / PlatformService.INSTANCE.fluidUnitsPerMillibucket();
    }
}
