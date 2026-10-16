package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;

/**
 * What a farm's {@link MachineTier} sets: how far its ring reaches and how large its buffers are.
 * Out-of-range values are normalised: the outer side is odd and wider than the
 * {@link FarmGeometry#COURTYARD}, capacities are at least 1 and the input limit never negative.
 */
public record FarmTierProfile(int outerSize, long energyCapacity, long maxEnergyInput, int waterCapacityMb) {

    public FarmTierProfile {
        outerSize = Math.max(FarmGeometry.COURTYARD + 2, outerSize | 1);
        energyCapacity = Math.max(1, energyCapacity);
        maxEnergyInput = Math.max(0, maxEnergyInput);
        waterCapacityMb = Math.max(1, waterCapacityMb);
    }

    /** The shipped profile of {@code tier}, which the config defaults to. */
    public static FarmTierProfile defaults(MachineTier tier) {
        return switch (tier) {
            case BASIC -> new FarmTierProfile(11, 20_000, 64, 8_000);
            case INDUSTRIAL -> new FarmTierProfile(15, 40_000, 128, 16_000);
            case PRECISION -> new FarmTierProfile(19, 80_000, 256, 32_000);
            case INFERNAL -> new FarmTierProfile(23, 160_000, 512, 64_000);
        };
    }

    public static FarmTierProfile fromConfig(MachineTier tier) {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.farmTier(tier);
        return new FarmTierProfile(
                LogisticsConfigHost.get(keys.outerSize()),
                LogisticsConfigHost.get(keys.energyCapacity()),
                LogisticsConfigHost.get(keys.maxEnergyInput()),
                LogisticsConfigHost.get(keys.waterCapacityMb()));
    }
}
