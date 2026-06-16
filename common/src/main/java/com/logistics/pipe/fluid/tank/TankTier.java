package com.logistics.pipe.fluid.tank;

import com.logistics.core.lib.fluids.FluidUnits;

/**
 * Capacity tiers for fluid tanks, expressed in buckets. Only {@link #GLASS} exists today; the enum is
 * the extension point for the rest of the ladder (copper/iron/gold/…) later.
 *
 * <p>Capacities are exposed in platform-native fluid units via {@link #capacityNative()}.
 */
public enum TankTier {
    GLASS(16);

    private final int buckets;

    TankTier(int buckets) {
        this.buckets = buckets;
    }

    /** Capacity in whole buckets, as shown in the tank tooltip. */
    public int buckets() {
        return buckets;
    }

    /** Capacity in platform-native units. */
    public long capacityNative() {
        return FluidUnits.mb(buckets * 1000);
    }

    /** One bottle = one third of a bucket, in platform-native units. */
    public static long bottleNative() {
        return FluidUnits.mb(1000) / 3;
    }
}
