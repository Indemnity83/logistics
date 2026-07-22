package com.logistics.power.engine.steam;

/**
 * The Steam Engine's firebox, derived each tick (never persisted) from the committed fuel reserve and
 * boiler heat. Once a fuel item is lit it burns continuously to completion; the engine controls
 * temperature only by deciding when to commit the next item.
 */
public enum SteamFireboxState {
    /** No committed reserve and heat at/below the refuel point (nothing to burn), or redstone-disabled. */
    OFF,
    /** No committed reserve, but the boiler is still above the refuel point: hot and ready, no new item yet. */
    STOKED,
    /** A committed reserve exists and is being consumed: fuel is lit and adding heat. */
    FIRING
}
