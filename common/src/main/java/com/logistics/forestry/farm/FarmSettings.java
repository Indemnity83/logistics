package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The farm's tunables in one snapshot. Production reads them from config each tick
 * ({@link #fromConfig()}); tests construct them directly. Out-of-range values are normalised, so
 * every snapshot is workable: intervals and rates are at least 1, costs never negative, the idle
 * back-off cap never below its base, every tier has a profile, and no tier's ring is narrower than
 * the tier below it.
 *
 * <p>A tier's energy capacity, maximum input and water capacity size the machine's buffers when the
 * block entity is built and whenever its tier changes; the rest apply live.
 */
public record FarmSettings(
        int workIntervalTicks,
        long rfHarvestPerBlock,
        long rfClearPerBlock,
        long rfPlant,
        long rfTill,
        long rfSoil,
        long rfMoisten,
        int waterPerTillMb,
        int harvestBlocksPerTick,
        int idleBackoffBaseTicks,
        int idleBackoffMaxTicks,
        Map<MachineTier, FarmTierProfile> tiers) {

    public FarmSettings {
        workIntervalTicks = Math.max(1, workIntervalTicks);
        rfHarvestPerBlock = Math.max(0, rfHarvestPerBlock);
        rfClearPerBlock = Math.max(0, rfClearPerBlock);
        rfPlant = Math.max(0, rfPlant);
        rfTill = Math.max(0, rfTill);
        rfSoil = Math.max(0, rfSoil);
        rfMoisten = Math.max(0, rfMoisten);
        waterPerTillMb = Math.max(0, waterPerTillMb);
        harvestBlocksPerTick = Math.max(1, harvestBlocksPerTick);
        idleBackoffBaseTicks = Math.max(1, idleBackoffBaseTicks);
        idleBackoffMaxTicks = Math.max(idleBackoffBaseTicks, idleBackoffMaxTicks);
        tiers = normalise(tiers);
    }

    /** The profile of {@code tier}. */
    public FarmTierProfile profile(MachineTier tier) {
        return tiers.get(tier);
    }

    /** {@code profile} at every tier. */
    public static Map<MachineTier, FarmTierProfile> everyTier(FarmTierProfile profile) {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, profile);
        }
        return tiers;
    }

    /** The shipped profile of every tier. */
    public static Map<MachineTier, FarmTierProfile> defaultTiers() {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, FarmTierProfile.defaults(tier));
        }
        return tiers;
    }

    public static FarmSettings fromConfig() {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, FarmTierProfile.fromConfig(tier));
        }
        return new FarmSettings(
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WORK_INTERVAL_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_HARVEST_PER_BLOCK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_CLEAR_PER_BLOCK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_PLANT),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_TILL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_SOIL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_MOISTEN),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WATER_PER_TILL_MB),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_HARVEST_BLOCKS_PER_TICK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS),
                tiers);
    }

    /** Fills missing tiers with their defaults and widens any ring narrower than the tier below it. */
    private static Map<MachineTier, FarmTierProfile> normalise(Map<MachineTier, FarmTierProfile> tiers) {
        Map<MachineTier, FarmTierProfile> result = new EnumMap<>(MachineTier.class);
        int floor = 0;
        for (MachineTier tier = MachineTier.BASIC; tier != null; tier = tier.next().orElse(null)) {
            FarmTierProfile profile = tiers.getOrDefault(tier, FarmTierProfile.defaults(tier));
            if (profile.outerSize() < floor) {
                profile = new FarmTierProfile(
                        floor, profile.energyCapacity(), profile.maxEnergyInput(), profile.waterCapacityMb());
            }
            floor = profile.outerSize();
            result.put(tier, profile);
        }
        return Collections.unmodifiableMap(result);
    }
}
