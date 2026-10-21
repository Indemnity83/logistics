package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;

/**
 * What a farm's {@link MachineTier} sets: how far its ring reaches, how fast it works and how large
 * its buffers are. A farm inspects up to {@code checksPerSecond} plots and performs up to
 * {@code actionsPerSecond} world-changing actions a second. Out-of-range values are normalised: the
 * outer side is odd and wider than the {@link FarmGeometry#COURTYARD}, rates and capacities are at
 * least 1 and the input limit never negative.
 */
public record FarmTierProfile(
        int outerSize,
        int checksPerSecond,
        int actionsPerSecond,
        long energyCapacity,
        long maxEnergyInput) {

    public FarmTierProfile {
        outerSize = Math.max(FarmGeometry.COURTYARD + 2, outerSize | 1);
        checksPerSecond = Math.max(1, checksPerSecond);
        actionsPerSecond = Math.max(1, actionsPerSecond);
        energyCapacity = Math.max(1, energyCapacity);
        maxEnergyInput = Math.max(0, maxEnergyInput);
    }

    /** The shipped profile of {@code tier}, which the config defaults to. */
    public static FarmTierProfile defaults(MachineTier tier) {
        return switch (tier) {
            case BASIC -> new FarmTierProfile(11, 16, 4, 20_000, 64);
            case INDUSTRIAL -> new FarmTierProfile(15, 32, 8, 40_000, 128);
            case PRECISION -> new FarmTierProfile(19, 64, 16, 80_000, 256);
            case INFERNAL -> new FarmTierProfile(23, 128, 32, 160_000, 512);
        };
    }

    public static FarmTierProfile fromConfig(MachineTier tier) {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.farmTier(tier);
        return new FarmTierProfile(
                LogisticsConfigHost.get(keys.outerSize()),
                LogisticsConfigHost.get(keys.checksPerSecond()),
                LogisticsConfigHost.get(keys.actionsPerSecond()),
                LogisticsConfigHost.get(keys.energyCapacity()),
                LogisticsConfigHost.get(keys.maxEnergyInput()));
    }

    /** This profile with its ring reaching {@code outerSize}. */
    public FarmTierProfile withOuterSize(int outerSize) {
        return new FarmTierProfile(outerSize, checksPerSecond, actionsPerSecond, energyCapacity, maxEnergyInput);
    }

    /** This profile working at {@code checksPerSecond} and {@code actionsPerSecond}. */
    public FarmTierProfile withRates(int checksPerSecond, int actionsPerSecond) {
        return new FarmTierProfile(outerSize, checksPerSecond, actionsPerSecond, energyCapacity, maxEnergyInput);
    }
}
