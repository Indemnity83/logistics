package com.logistics.core.lib.power;

/** Two-stroke engine cycle phases. */
public enum CyclePhase {
    IDLE,
    EXPANSION,
    COMPRESSION;

    private static final CyclePhase[] VALUES = values();

    public static CyclePhase fromOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : IDLE;
    }
}
