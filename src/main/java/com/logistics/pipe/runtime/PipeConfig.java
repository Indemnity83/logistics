package com.logistics.pipe.runtime;

public class PipeConfig {
    // Constant speed added per tick when a pipe applies acceleration (e.g., powered boost pipes).
    // This is a linear delta, not a multiplier, so larger values ramp speed faster each tick.
    // 1/200 blocks per tick^2 means +0.005 blocks/tick after one tick of acceleration.
    public static final float ACCELERATION_RATE = 1.0f / 200.0f;

    // Fraction of the current speed removed per tick when not accelerating.
    // This creates a smooth exponential decay (speed -= speed * DRAG_COEFFICIENT).
    // Tuned so one fully-powered boost segment (starting at ITEM_MIN_SPEED with ACCELERATION_RATE)
    // keeps the item just above ITEM_MIN_SPEED after ~15 more unpowered segments.
    public static final float DRAG_COEFFICIENT = 0.00536f;

    // Ticks between extraction attempts for extractor pipes.
    // Larger values reduce throughput but also reduce work per tick.
    public static final int EXTRACTION_INTERVAL = 60;

    // Hard floor for item speed while traveling through pipes.
    // Items will never slow below this, even under drag, so movement doesn't stall.
    // 1/60 blocks per tick = 1.2 blocks/sec = 60 ticks per block.
    public static final float ITEM_MIN_SPEED = 1.0f / 60.0f;

    // Default ceiling for item speed while traveling through pipes.
    // Individual pipes can override this up or down via getMaxSpeed.
    // 1/4 blocks per tick = 5 blocks/sec = 4 ticks per block.
    public static final float PIPE_MAX_SPEED = 1.0f / 4.0f;
}
