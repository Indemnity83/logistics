package com.logistics.power.engine.magmatic;

/**
 * Centralized, tunable balance profile for a Magmatic Engine — the single place the simulation reads its
 * numbers from. Output/buffer/tank/burn-duration come from config; the heat-soak/cooling rates and the
 * four hysteresis thresholds default here.
 *
 * <p>Total RF per lava bucket is <em>not</em> a fixed value: it is burn duration ({@link #bucketBurnTicks})
 * times the continuous, temperature-driven {@link #outputAtTemperature(double)} — a cold engine is
 * inefficient, a fully heat-soaked one reaches 150%. {@code heatSoakRate} and {@code bucketBurnTicks} are
 * deliberately independent axes (warm-up speed vs. how long a batch lasts).
 */
public record MagmaticEngineProfile(
        long baseOutputPerTick,
        long bufferCapacity,
        int lavaTankCapacityMb,
        int batchMb,
        int bucketBurnTicks,
        double heatSoakRate,
        double coolingRate,
        double coldToWarmThreshold,
        double warmToColdThreshold,
        double warmToHotThreshold,
        double hotToWarmThreshold) {

    /** Builds a profile from the config-backed values, defaulting the batch size, rates, and thresholds. */
    public static MagmaticEngineProfile of(
            long baseOutputPerTick, long bufferCapacity, int lavaTankCapacityMb, int bucketBurnTicks) {
        return new MagmaticEngineProfile(
                baseOutputPerTick, bufferCapacity, lavaTankCapacityMb, 100, bucketBurnTicks,
                0.0025, 0.0010, 0.27, 0.23, 0.77, 0.73);
    }

    /** Powered burn ticks a single committed batch lasts (2,000 for 100 mB of 20,000-tick lava); never below 1. */
    public int batchBurnTicks() {
        return Math.max(1, bucketBurnTicks * batchMb / 1000);
    }

    /** RF/t at ambient (t=0). */
    public long coldOutputPerTick() {
        return Math.round(baseOutputPerTick * 0.5);
    }

    /** RF/t at furnace parity (t=0.5). */
    public long warmOutputPerTick() {
        return baseOutputPerTick;
    }

    /** RF/t fully heat-soaked (t=1). */
    public long hotOutputPerTick() {
        return Math.round(baseOutputPerTick * 1.5);
    }

    /** Conservative max RF a single batch could ever produce — the ignition admission requirement. */
    public long maximumBatchPotentialRf() {
        return hotOutputPerTick() * (long) batchBurnTicks();
    }

    /**
     * Continuous output for a normalized temperature in {@code [0,1]} — the single place output↔temperature
     * interpolation and rounding live (keeps balance math out of the tick, and stage out of generation).
     */
    public long outputAtTemperature(double temperature) {
        double t = Math.clamp(temperature, 0.0, 1.0);
        return Math.round(coldOutputPerTick() + t * (hotOutputPerTick() - coldOutputPerTick()));
    }

    /**
     * Midpoint of the Cold↔Warm hysteresis gap — the stateless display-band boundary used to seed the
     * (unpersisted) stage on load. Distinct from the {@code *Threshold} transition values so a temperature
     * inside the hysteresis gap is never ambiguous.
     */
    public double warmBandFloor() {
        return (coldToWarmThreshold + warmToColdThreshold) / 2.0;
    }

    /** Midpoint of the Warm↔Hot hysteresis gap — the stateless Warm→Hot display-band boundary for load seeding. */
    public double hotBandFloor() {
        return (warmToHotThreshold + hotToWarmThreshold) / 2.0;
    }
}
