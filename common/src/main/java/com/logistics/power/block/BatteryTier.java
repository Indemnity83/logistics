package com.logistics.power.block;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;

/**
 * Storage grade of a battery. Each tier is its own block sharing one block entity type,
 * mirroring {@link com.logistics.power.cable.CableTier}.
 *
 * <p>{@link #BASIC} carries the values the single untiered Battery shipped with, so a world
 * saved before the line existed loads at its original capacity and throughput.
 *
 * <p>The names are the ladder's tier adjectives rather than material names, matching
 * {@link com.logistics.power.cable.CableTier}: the bodies above Basic are invented alloys whose
 * names carry no rank, so the rank lives in the name.
 */
public enum BatteryTier {
    BASIC("basic_battery", "Basic Battery"),
    CONDUCTIVE("conductive_battery", "Conductive Battery"),
    RESONANT("resonant_battery", "Resonant Battery"),
    DEEP("deep_battery", "Deep Battery");

    private final String id;
    private final String displayName;

    BatteryTier(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Total RF storage, sourced from the {@code power/battery/<tier>} config. */
    public long capacity() {
        return switch (this) {
            case BASIC -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY);
            case CONDUCTIVE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_CONDUCTIVE_CAPACITY);
            case RESONANT -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_RESONANT_CAPACITY);
            case DEEP -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_DEEP_CAPACITY);
        };
    }

    /** Max RF/t inserted or extracted per side. */
    public long maxIo() {
        return switch (this) {
            case BASIC -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BASIC_MAX_IO);
            case CONDUCTIVE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_CONDUCTIVE_MAX_IO);
            case RESONANT -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_RESONANT_MAX_IO);
            case DEEP -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_DEEP_MAX_IO);
        };
    }

    /** Max RF/t actively pushed into each adjacent machine. */
    public long outputPerSide() {
        return switch (this) {
            case BASIC -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BASIC_OUTPUT_PER_SIDE);
            case CONDUCTIVE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_CONDUCTIVE_OUTPUT_PER_SIDE);
            case RESONANT -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_RESONANT_OUTPUT_PER_SIDE);
            case DEEP -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_DEEP_OUTPUT_PER_SIDE);
        };
    }
}
