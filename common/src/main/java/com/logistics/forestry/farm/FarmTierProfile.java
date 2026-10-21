package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;

/**
 * What a farm's {@link MachineTier} sets: how fast it works and how large its buffer is. A farm
 * inspects up to {@code checksPerSecond} positions and performs up to {@code actionsPerSecond}
 * world-changing actions a second. Out-of-range values are normalised: rates and capacities are at
 * least 1 and the input limit never negative.
 */
public record FarmTierProfile(int checksPerSecond, int actionsPerSecond, long energyCapacity, long maxEnergyInput) {

    public FarmTierProfile {
        checksPerSecond = Math.max(1, checksPerSecond);
        actionsPerSecond = Math.max(1, actionsPerSecond);
        energyCapacity = Math.max(1, energyCapacity);
        maxEnergyInput = Math.max(0, maxEnergyInput);
    }

    /** The shipped profile of {@code tier}, which the config defaults to. */
    public static FarmTierProfile defaults(MachineTier tier) {
        return switch (tier) {
            case BASIC -> new FarmTierProfile(16, 4, 20_000, 64);
            case INDUSTRIAL -> new FarmTierProfile(32, 8, 40_000, 128);
            case PRECISION -> new FarmTierProfile(64, 16, 80_000, 256);
            case INFERNAL -> new FarmTierProfile(128, 32, 160_000, 512);
        };
    }

    public static FarmTierProfile fromConfig(MachineTier tier) {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.farmTier(tier);
        return new FarmTierProfile(
                LogisticsConfigHost.get(keys.checksPerSecond()),
                LogisticsConfigHost.get(keys.actionsPerSecond()),
                LogisticsConfigHost.get(keys.energyCapacity()),
                LogisticsConfigHost.get(keys.maxEnergyInput()));
    }

    /** This profile working at {@code checksPerSecond} and {@code actionsPerSecond}. */
    public FarmTierProfile withRates(int checksPerSecond, int actionsPerSecond) {
        return new FarmTierProfile(checksPerSecond, actionsPerSecond, energyCapacity, maxEnergyInput);
    }
}
