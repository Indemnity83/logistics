package com.logistics.core.lib.fluids;

import com.logistics.core.lib.platform.PlatformService;

/**
 * Converts author-facing millibucket (mB) amounts into platform-native fluid units.
 *
 * <p>Fluid storage amounts are platform-native (Fabric droplets = 81/mB, NeoForge mB = 1/mB),
 * so capacities and transfer rates expressed in mB in config must be converted at the world
 * boundary before being handed to {@link com.logistics.core.lib.fluids.IFluidStorage}.
 *
 * <p>The public conversions read the loader factor from {@link PlatformService}, so production code
 * outside tests operates directly in native units; the package-private overloads take the factor
 * explicitly and are unit-tested directly.
 */
public final class FluidUnits {

    private FluidUnits() {}

    /** Converts a millibucket amount to platform-native fluid units. */
    public static long mb(long millibuckets) {
        return mb(millibuckets, PlatformService.INSTANCE.fluidUnitsPerMillibucket());
    }

    /** Converts platform-native fluid units back to millibuckets, rounding down. */
    public static long toMillibuckets(long nativeUnits) {
        return toMillibuckets(nativeUnits, PlatformService.INSTANCE.fluidUnitsPerMillibucket());
    }

    /**
     * Converts platform-native fluid units back to millibuckets, rounding up. Use this for an amount that
     * left our own storage: a handler may accept a fraction of a millibucket (a cauldron level is a third of
     * a bucket, which is not a whole number of mB on Fabric's 81-droplet scale), and rounding that down would
     * leave us still counting fluid we no longer hold.
     */
    public static long toMillibucketsCeil(long nativeUnits) {
        return toMillibucketsCeil(nativeUnits, PlatformService.INSTANCE.fluidUnitsPerMillibucket());
    }

    // Package-private overloads take the factor explicitly so the conversion math is unit-testable
    // without a bootstrapped loader PlatformService.

    static long mb(long millibuckets, long factor) {
        return Math.multiplyExact(millibuckets, requirePositive(factor));
    }

    static long toMillibuckets(long nativeUnits, long factor) {
        return nativeUnits / requirePositive(factor);
    }

    static long toMillibucketsCeil(long nativeUnits, long factor) {
        long positive = requirePositive(factor);
        return Math.floorDiv(nativeUnits + positive - 1, positive);
    }

    private static long requirePositive(long factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("fluidUnitsPerMillibucket must be positive, was " + factor);
        }
        return factor;
    }
}
