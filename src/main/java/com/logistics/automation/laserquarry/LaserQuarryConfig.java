package com.logistics.automation.laserquarry;

/**
 * Configuration constants for laser quarry operation and rendering.
 */
public final class LaserQuarryConfig {
    private LaserQuarryConfig() {}

    // Size of the quarry mining area (16x16 blocks).
    public static final int CHUNK_SIZE = 16;

    // Mining area is inset 1 block from the frame on each side (14x14 blocks).
    public static final int INNER_SIZE = 14;

    // Height of the frame above the quarry block.
    public static final int Y_OFFSET_ABOVE = 4;

    // Legacy arm speed constant for client-side interpolation fallback.
    public static final float ARM_SPEED = 0.1f;

    // Maximum blocks to skip per tick when scanning for mineable blocks.
    // Prevents lag spikes when traversing large air pockets.
    public static final int MAX_SKIP_PER_TICK = 256;

    // ==================== Energy Configuration ====================
    // BuildCraft-style energy system with self-balancing consumption.
    // Higher buffer = higher consumption = faster operation.

    // Mining multiplier (1.0 = default, can be adjusted for balance).
    public static final double MINING_MULTIPLIER = 1.0;

    // Base energy cost to break a block (before hardness multiplier).
    // Used to derive buffer capacity using BC formula.
    public static final long BREAK_ENERGY = 60L;

    // Energy buffer capacity: 2 * 64 * BREAK_ENERGY * miningMultiplier RF
    // This formula ensures that at full buffer, consumption (788 RF/t) < input (1000 RF/t),
    // allowing the quarry to maintain full speed with adequate power supply.
    public static final long ENERGY_CAPACITY = (long) (2 * 64 * BREAK_ENERGY * MINING_MULTIPLIER);

    // Maximum energy input rate: 1,000 × miningMultiplier RF/tick
    public static final long MAX_ENERGY_INPUT = (long) (1_000L * MINING_MULTIPLIER);

    // Frame block build cost: 240 RF (fixed)
    public static final long FRAME_BUILD_COST = 240L;

    // Base move cost: 20 RF/tick minimum
    public static final long BASE_MOVE_COST = 20L;

    // Move cost scales with buffer: ceil(20 + buffer/10) RF/tick
    public static final long MOVE_COST_BUFFER_DIVISOR = 10L;

    // Break energy formula (matches BC): BREAK_ENERGY * miningMultiplier * ((hardness + 1) * 2)
    // Stone (1.5 hardness): 60 * 1.0 * 5 = 300 RF
    // This is the TOTAL energy needed to break a block, not per-tick cost.
    public static final double BREAK_ENERGY_MULTIPLIER = BREAK_ENERGY * MINING_MULTIPLIER * 2;

    // Speed formula: 0.1 + (energyUsed / 2000) blocks/tick
    public static final float BASE_MOVE_SPEED = 0.1f;
    public static final float SPEED_ENERGY_DIVISOR = 2000f;

    // Rain penalty: 0.7× speed multiplier when raining
    public static final float RAIN_SPEED_MULTIPLIER = 0.7f;
}
