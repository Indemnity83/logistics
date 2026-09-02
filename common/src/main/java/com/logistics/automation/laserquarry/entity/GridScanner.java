package com.logistics.automation.laserquarry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Pure scan/position math for the laser quarry's clearing and mining phases.
 * Resolves a grid index into a world-space target {@link BlockPos} given the
 * quarry's facing and bounds, and decides whether a block is skippable.
 */
public final class GridScanner {

    private GridScanner() {}

    /**
     * Target position for the clearing phase (area at and above the quarry level).
     * Returns {@code null} when the grid index walks below the quarry level (clearing done).
     */
    public static @Nullable BlockPos clearingTarget(
            Direction facing,
            BlockPos quarryPos,
            QuarryBounds bounds,
            int defaultArea,
            int gridX,
            int gridY,
            int gridZ) {
        QuarryFrameRect rect = QuarryFrameRect.resolve(facing, quarryPos, bounds, defaultArea);
        if (rect == null) {
            return null;
        }
        int currentY = rect.topY() - gridY;
        if (currentY < rect.bottomY()) {
            return null;
        }
        return new BlockPos(rect.startX() + gridX, currentY, rect.startZ() + gridZ);
    }

    /**
     * Target position for the mining phase (1-block-inset interior below the quarry level).
     * Walks a 3D zigzag pattern that reverses each layer to minimize arm travel.
     * Returns {@code null} when the world bottom or end of area is reached.
     */
    public static @Nullable BlockPos miningTarget(
            Direction facing,
            BlockPos quarryPos,
            QuarryBounds bounds,
            int defaultArea,
            int worldMinY,
            int gridX,
            int gridY,
            int gridZ) {
        QuarryFrameRect rect = QuarryFrameRect.resolve(facing, quarryPos, bounds, defaultArea);
        if (rect == null) {
            return null;
        }
        int innerWidth = rect.innerWidth();
        int innerDepth = rect.innerDepth();
        if (innerWidth <= 0 || innerDepth <= 0) {
            return null;
        }

        int startX = rect.startX() + 1;
        int startZ = rect.startZ() + 1;
        int currentY = rect.bottomY() - 1 - gridY;
        if (currentY < worldMinY) {
            return null;
        }

        int targetZ = (gridY % 2 == 0) ? startZ + gridZ : startZ + (innerDepth - 1 - gridZ);

        int totalRows = gridY * innerDepth + gridZ;
        int targetX = (totalRows % 2 == 0) ? startX + gridX : startX + (innerWidth - 1 - gridX);

        return new BlockPos(targetX, currentY, targetZ);
    }

    /**
     * True if the block at {@code pos} cannot or should not be broken by the quarry.
     * Skips air, fluids, and unbreakable blocks (bedrock, barriers, etc.) — treats lava as just
     * another skippable cell. Callers use {@link #isHazardousFluid} separately to recognize it.
     */
    public static boolean shouldSkip(BlockGetter world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        return state.getDestroySpeed(world, pos) < 0;
    }

    /**
     * True if {@code state} is a fluid the quarry must treat as an unminable obstruction — like
     * bedrock, never broken and never traveled through. Compares fluid identity directly rather
     * than the {@code minecraft:lava} tag — tags require data-pack binding that isn't available
     * in the plain unit-test bootstrap.
     */
    public static boolean isHazardousFluid(BlockState state) {
        var fluid = state.getFluidState().getType();
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }
}
