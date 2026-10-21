package com.logistics.forestry.farm;

import java.util.Locale;

/**
 * What a farm is doing, as the GUI and Jade report it. Synced to the client by ordinal, so new
 * values go at the end.
 */
public enum FarmStatus {
    WORKING,
    /** Nothing ripe anywhere in the Reach. */
    IDLE,
    NO_POWER,
    /** Stopped by its redstone control. */
    PAUSED,
    /** Claim protection or the world refused a harvest or a replant. */
    BLOCKED,
    /** The inventory is full; the farm keeps working and drops what does not fit. */
    INVENTORY_FULL,
    /** Empty Plots are waiting for their germlings to arrive in the inventory. */
    WAITING_FOR_SEEDS;

    private static final FarmStatus[] VALUES = values();

    public String translationKey() {
        return "status.logistics.farm." + name().toLowerCase(Locale.ROOT);
    }

    /** The status with {@code ordinal}, or {@link #IDLE} for an unknown one. */
    public static FarmStatus byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : IDLE;
    }
}
