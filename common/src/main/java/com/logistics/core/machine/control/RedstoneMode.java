package com.logistics.core.machine.control;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * How a machine or engine reacts to a redstone signal. Each mode carries an explicit serialized name
 * (NBT) and sync id (menu protocol); neither is derived from the ordinal, so reordering is safe.
 */
public enum RedstoneMode {
    /** Runs regardless of the signal. */
    IGNORED("ignored", 0),
    /** Runs only while unpowered. */
    LOW("low", 1),
    /** Runs only while powered. */
    HIGH("high", 2);

    private final String serializedName;
    private final int syncId;

    RedstoneMode(String serializedName, int syncId) {
        this.serializedName = serializedName;
        this.syncId = syncId;
    }

    public String serializedName() {
        return serializedName;
    }

    public int syncId() {
        return syncId;
    }

    /** Whether work is permitted given the current signal. */
    public boolean allows(boolean signal) {
        return switch (this) {
            case IGNORED -> true;
            case LOW -> !signal;
            case HIGH -> signal;
        };
    }

    /** The mode with {@code name} as its serialized name, or {@code fallback} for null/unknown names. */
    public static RedstoneMode byName(@Nullable String name, RedstoneMode fallback) {
        for (RedstoneMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return fallback;
    }

    /** The mode with {@code syncId}, or empty when no mode has it. */
    public static Optional<RedstoneMode> bySyncId(int syncId) {
        for (RedstoneMode mode : values()) {
            if (mode.syncId == syncId) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
