package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;

/**
 * The farm's tunables in one snapshot. Production reads them from config each tick
 * ({@link #fromConfig()}); tests construct them directly. Out-of-range values are normalised, so
 * every snapshot is workable: intervals and rates are at least 1, costs never negative, and the
 * idle back-off cap never below its base.
 *
 * <p>Energy capacity, maximum input and water capacity size the machine's buffers when the block
 * entity is built; the rest apply live.
 */
public record FarmSettings(
        int workIntervalTicks,
        long energyCapacity,
        long maxEnergyInput,
        long rfHarvestPerBlock,
        long rfClearPerBlock,
        long rfPlant,
        long rfTill,
        long rfSoil,
        long rfMoisten,
        int waterCapacityMb,
        int waterPerTillMb,
        int harvestBlocksPerTick,
        int idleBackoffBaseTicks,
        int idleBackoffMaxTicks) {

    public FarmSettings {
        workIntervalTicks = Math.max(1, workIntervalTicks);
        energyCapacity = Math.max(1, energyCapacity);
        maxEnergyInput = Math.max(0, maxEnergyInput);
        rfHarvestPerBlock = Math.max(0, rfHarvestPerBlock);
        rfClearPerBlock = Math.max(0, rfClearPerBlock);
        rfPlant = Math.max(0, rfPlant);
        rfTill = Math.max(0, rfTill);
        rfSoil = Math.max(0, rfSoil);
        rfMoisten = Math.max(0, rfMoisten);
        waterCapacityMb = Math.max(1, waterCapacityMb);
        waterPerTillMb = Math.max(0, waterPerTillMb);
        harvestBlocksPerTick = Math.max(1, harvestBlocksPerTick);
        idleBackoffBaseTicks = Math.max(1, idleBackoffBaseTicks);
        idleBackoffMaxTicks = Math.max(idleBackoffBaseTicks, idleBackoffMaxTicks);
    }

    public static FarmSettings fromConfig() {
        return new FarmSettings(
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WORK_INTERVAL_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_ENERGY_CAPACITY),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_MAX_ENERGY_INPUT),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_HARVEST_PER_BLOCK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_CLEAR_PER_BLOCK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_PLANT),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_TILL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_SOIL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_MOISTEN),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WATER_CAPACITY_MB),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WATER_PER_TILL_MB),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_HARVEST_BLOCKS_PER_TICK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS));
    }
}
