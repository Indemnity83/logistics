package com.logistics.quarry;

/**
 * Configuration constants for quarry operation and rendering.
 */
public final class QuarryConfig {
    private QuarryConfig() {}

    // Size of the quarry mining area (16x16 blocks).
    public static final int CHUNK_SIZE = 16;

    // Mining area is inset 1 block from the frame on each side (14x14 blocks).
    public static final int INNER_SIZE = 14;

    // Height of the frame above the quarry block.
    public static final int Y_OFFSET_ABOVE = 4;

    // Number of tool slots in the quarry inventory.
    public static final int INVENTORY_SIZE = 9;

    // Arm movement speed in blocks per tick.
    // 0.1 blocks/tick = 2 blocks/second at 20 TPS.
    public static final float ARM_SPEED = 0.1f;

    // Maximum blocks to skip per tick when scanning for mineable blocks.
    // Prevents lag spikes when traversing large air pockets.
    public static final int MAX_SKIP_PER_TICK = 256;
}
