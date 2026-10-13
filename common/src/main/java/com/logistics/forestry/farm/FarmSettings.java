package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;

/**
 * The farm's tunables in one snapshot. Production reads them from config each tick
 * ({@link #fromConfig()}); tests construct them directly. Out-of-range values are normalised, so
 * every snapshot is workable: the area is odd and at least 1, intervals and rates at least 1, and
 * the idle back-off cap never below its base.
 *
 * <p>Energy capacity, maximum input and water capacity size the machine's buffers when the block
 * entity is built; the rest apply live.
 */
public record FarmSettings(
        int areaSize,
        int workIntervalTicks,
        long energyCapacity,
        long maxEnergyInput,
        long rfHarvestPerBlock,
        long rfPlant,
        long rfTill,
        long rfSoil,
        long rfIrrigate,
        int waterCapacityMb,
        int waterPerIrrigateMb,
        int harvestBlocksPerTick,
        int idleBackoffBaseTicks,
        int idleBackoffMaxTicks) {

    public FarmSettings {
        areaSize = Math.max(1, areaSize) | 1;
        workIntervalTicks = Math.max(1, workIntervalTicks);
        energyCapacity = Math.max(1, energyCapacity);
        maxEnergyInput = Math.max(0, maxEnergyInput);
        rfHarvestPerBlock = Math.max(0, rfHarvestPerBlock);
        rfPlant = Math.max(0, rfPlant);
        rfTill = Math.max(0, rfTill);
        rfSoil = Math.max(0, rfSoil);
        rfIrrigate = Math.max(0, rfIrrigate);
        waterCapacityMb = Math.max(1, waterCapacityMb);
        waterPerIrrigateMb = Math.max(0, waterPerIrrigateMb);
        harvestBlocksPerTick = Math.max(1, harvestBlocksPerTick);
        idleBackoffBaseTicks = Math.max(1, idleBackoffBaseTicks);
        idleBackoffMaxTicks = Math.max(idleBackoffBaseTicks, idleBackoffMaxTicks);
    }

    public static FarmSettings fromConfig() {
        return new FarmSettings(
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_AREA_SIZE),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WORK_INTERVAL_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_ENERGY_CAPACITY),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_MAX_ENERGY_INPUT),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_HARVEST_PER_BLOCK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_PLANT),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_TILL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_SOIL),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_RF_IRRIGATE),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WATER_CAPACITY_MB),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_WATER_PER_IRRIGATE_MB),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_HARVEST_BLOCKS_PER_TICK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS));
    }
}
