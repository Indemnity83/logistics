package com.logistics.automation.laserquarry;

/**
 * Shared geometry constants for the laser quarry structure.
 * Used across the block, block entity, frame block, and renderer.
 *
 * <p>Configurable tuning values live in the configory {@code machines} config ({@code quarry_*} keys).
 */
public final class LaserQuarryGeometry {
    private LaserQuarryGeometry() {}

    // Height of the frame above the quarry block.
    public static final int Y_OFFSET_ABOVE = 4;
}
