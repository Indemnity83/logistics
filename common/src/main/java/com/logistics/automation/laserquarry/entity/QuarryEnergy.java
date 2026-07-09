package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;

/**
 * Derived quarry energy values, computed from the {@code quarry_energy_per_block} and
 * {@code quarry_energy_multiplier} config keys. The finite/non-negative clamps are load-bearing: a
 * hand-edited NaN or negative multiplier must not divide-by-zero in {@code getEnergyLevel()}.
 */
public final class QuarryEnergy {
    private QuarryEnergy() {}

    public static double energyPerBlockMultiplier() {
        return nonNegativeFiniteOrZero(energyPerBlock() * energyMultiplier() * 2);
    }

    public static long energyCapacity() {
        return nonNegativeLongOrZero(128.0 * energyPerBlock() * energyMultiplier());
    }

    public static long maxEnergyInput() {
        return nonNegativeLongOrZero(1_000L * energyMultiplier());
    }

    private static long energyPerBlock() {
        return LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_ENERGY_PER_BLOCK);
    }

    private static double energyMultiplier() {
        return LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_ENERGY_MULTIPLIER);
    }

    private static double nonNegativeFiniteOrZero(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static long nonNegativeLongOrZero(double value) {
        if (!Double.isFinite(value)) return 0L;
        return Math.max(0L, (long) value);
    }
}
