package com.logistics.pipe.block;

/**
 * How a fluid pipe relates to the block on one of its sides.
 *
 * <p>This is the fluid network's own connection classification, deliberately separate from the
 * item pipes' {@code PipeConnection.Type}, so fluid pipes never connect to item or energy pipes.
 */
public enum FluidConnection {
    /** No fluid connection (empty side, wrench-disabled, or a non-fluid neighbour). */
    NONE,
    /** Connected to another fluid pipe. */
    PIPE,
    /** Connected to an external fluid handler (tank, machine, engine). */
    HANDLER;

    private static final FluidConnection[] VALUES = values();

    public static FluidConnection byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }
}
