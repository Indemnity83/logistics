package com.logistics.power.engine.steam;

/**
 * A derived, human-facing summary of what the Steam Engine is doing right now (never persisted).
 * Shown as the primary status line in the GUI and Jade HUD, independent of {@link SteamFireboxState}.
 */
public enum SteamEngineStatus {
    /** Redstone-disabled: paused, reserve preserved, pressure cooling away. */
    REDSTONE_DISABLED,
    /** Below relight pressure with no reserve and no fuel item to ignite. */
    NO_FUEL,
    /** A lit firebox that can't boil because the water tank is empty. */
    NO_WATER,
    /** Boiling to build pressure up toward the operating threshold. */
    BUILDING_PRESSURE,
    /** Delivering RF (pressure at/above operating, or on the ramp). */
    GENERATING,
    /** Holding pressure but not delivering RF this tick (no demand / buffer). */
    COASTING,
    /** Pressure is available but the output-face neighbor accepted nothing (no/full/disconnected consumer). */
    OUTPUT_BLOCKED,
    /** No pressure, no reserve — completely idle. */
    EMPTY
}
