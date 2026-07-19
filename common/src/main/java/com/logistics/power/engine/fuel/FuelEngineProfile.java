package com.logistics.power.engine.fuel;

/**
 * Centralized, tunable balance profile for a Fuel Engine — the single place the simulation reads its
 * numbers from. RF/temperature/waste values come from config; the cooling ratios/rate default here.
 */
public record FuelEngineProfile(
        long minGeneration,
        long maxGeneration,
        double maxTemperature,
        double wasteHeatPerRejectedRf,
        double passiveCoolingRate,
        double passiveCoolingThresholdRatio,
        double restartTemperatureRatio,
        int fuelBatchMb,
        int coolantBatchMb) {

    /** Builds a profile from the config-backed values, defaulting the cooling ratios/rate + batch sizes. */
    public static FuelEngineProfile of(
            long minGeneration, long maxGeneration, double maxTemperature, double wasteHeatPerRejectedRf) {
        return new FuelEngineProfile(
                minGeneration, maxGeneration, maxTemperature, wasteHeatPerRejectedRf, 0.5, 0.5, 0.75, 100, 100);
    }

    /** Temperature above which an idle engine may still commit a fresh coolant batch. */
    public double passiveCoolingTarget() {
        return maxTemperature * passiveCoolingThresholdRatio;
    }

    /** Temperature an overheated engine is capped to on wrench reset (never reheats a cooler one). */
    public double restartTemperature() {
        return maxTemperature * restartTemperatureRatio;
    }
}
