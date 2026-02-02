package com.logistics.pipe.runtime;

public final class PipeConfig {
    private PipeConfig() {}

    // Constant speed added per tick when a pipe applies acceleration (e.g., powered boost pipes).
    // This is a linear delta, not a multiplier, so larger values ramp speed faster each tick.
    // 1/200 blocks per tick^2 means +0.005 blocks/tick after one tick of acceleration.
    public static final float ACCELERATION_RATE = 1.0f / 200.0f;

    // Fraction of the current speed removed per tick when not accelerating.
    // This creates a smooth exponential decay (speed -= speed * DRAG_COEFFICIENT).
    // Tuned so one fully-powered boost segment (starting at ITEM_MIN_SPEED with ACCELERATION_RATE)
    // keeps the item just above ITEM_MIN_SPEED after ~15 more unpowered segments.
    public static final float DRAG_COEFFICIENT = 0.005f;

    // Hard floor for item speed while traveling through pipes.
    // Items will never slow below this, even under drag, so movement doesn't stall.
    public static final float ITEM_MIN_SPEED = 0.02f;

    // Default ceiling for item speed while traveling through pipes.
    // Individual pipes can override this up or down via getMaxSpeed.
    public static final float PIPE_MAX_SPEED = 0.16f;
}
