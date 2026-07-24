package com.logistics.power.engine.steam;

/**
 * A derived, human-facing summary of what the Steam Engine is doing right now (never persisted).
 * Shown as the primary status line in the GUI and Jade HUD, independent of {@link SteamFireboxState}.
 */
public enum SteamEngineStatus {
    /** Redstone-disabled: paused, committed reserve preserved, heat and eventually pressure bleeding away. */
    REDSTONE_DISABLED,
    /** No committed reserve, heat at/below the refuel point, and no fuel item to commit. */
    NO_FUEL,
    /** Warming up: a reserve is burning but the boiler is still below the boiling point (no steam yet). */
    HEATING,
    /** Hot enough to boil but the water tank is empty. */
    NO_WATER,
    /** Boiling to build pressure up toward the operating threshold. */
    BUILDING_PRESSURE,
    /** Delivering RF (pressure at/above operating, or on the ramp). */
    GENERATING,
    /** Holding pressure but not delivering RF this tick (no demand / consumer). */
    COASTING,
    /** Pressure is available but the output-face neighbor accepted nothing (no/full/disconnected consumer). */
    OUTPUT_BLOCKED,
    /** No pressure, no reserve — completely idle. */
    EMPTY
}
