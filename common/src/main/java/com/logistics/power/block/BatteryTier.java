package com.logistics.power.block;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;

/**
 * Storage grade of a battery. Each tier is its own block sharing one block entity type,
 * mirroring {@link com.logistics.power.cable.CableTier}.
 *
 * <p>{@link #COPPER} carries the values the single untiered Battery shipped with, so a world
 * saved before the line existed loads at its original capacity and throughput.
 */
public enum BatteryTier {
    COPPER("copper_battery", "Copper Battery"),
    BRONZE("bronze_battery", "Bronze Battery"),
    GOLD("gold_battery", "Gold Battery"),
    AMETHYST("amethyst_battery", "Amethyst Battery"),
    ECHO("echo_battery", "Echo Battery");

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
            case COPPER -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_COPPER_CAPACITY);
            case BRONZE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BRONZE_CAPACITY);
            case GOLD -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_GOLD_CAPACITY);
            case AMETHYST -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_AMETHYST_CAPACITY);
            case ECHO -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_ECHO_CAPACITY);
        };
    }

    /** Max RF/t inserted or extracted per side. */
    public long maxIo() {
        return switch (this) {
            case COPPER -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_COPPER_MAX_IO);
            case BRONZE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BRONZE_MAX_IO);
            case GOLD -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_GOLD_MAX_IO);
            case AMETHYST -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_AMETHYST_MAX_IO);
            case ECHO -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_ECHO_MAX_IO);
        };
    }

    /** Max RF/t actively pushed into each adjacent machine. */
    public long outputPerSide() {
        return switch (this) {
            case COPPER -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_COPPER_OUTPUT_PER_SIDE);
            case BRONZE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_BRONZE_OUTPUT_PER_SIDE);
            case GOLD -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_GOLD_OUTPUT_PER_SIDE);
            case AMETHYST -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_AMETHYST_OUTPUT_PER_SIDE);
            case ECHO -> LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_ECHO_OUTPUT_PER_SIDE);
        };
    }
}
