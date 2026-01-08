package com.logistics.pipe.runtime;

public class PipeConfig {
    // Wooden pipes are slower - 3 seconds to traverse
    public static final float EXTRACTED_ITEM_SPEED = 1.0f / 60.0f; // Blocks per tick

    // Acceleration rate - how quickly items adjust to pipe speed
    public static final float ACCELERATION_RATE = 0.004f; // Speed change per tick (doubled)

    public static final int EXTRACTION_INTERVAL = 60;

    // Base pipe speed - matches the speed after ~3 powered gold pipes
    // At 0.15 blocks/tick, takes ~6.67 ticks (0.33 seconds) to traverse one segment
    public static final float BASE_PIPE_SPEED = 3.0f / 20.0f; // Blocks per tick (0.15)
}
