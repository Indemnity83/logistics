package com.logistics.forestry.farm;

import java.util.Locale;

/**
 * What a farm is doing, as the GUI and Jade report it. Synced to the client by ordinal, so new
 * values go at the end.
 */
public enum FarmStatus {
    WORKING,
    /** Nothing to do anywhere in the plot. */
    IDLE,
    NO_POWER,
    NO_WATER,
    NO_GERMLINGS,
    NO_SOIL,
    /** The output buffer is full; harvesting waits, other work goes on. */
    OUTPUT_FULL,
    /** Stopped by its redstone control. */
    PAUSED,
    TOO_DARK,
    BLOCKED,
    /** Placed, not started: surveying the ring until Start is pressed. */
    SURVEYING;

    private static final FarmStatus[] VALUES = values();

    public String translationKey() {
        return "status.logistics.farm." + name().toLowerCase(Locale.ROOT);
    }

    /** The status with {@code ordinal}, or {@link #IDLE} for an unknown one. */
    public static FarmStatus byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : IDLE;
    }
}
