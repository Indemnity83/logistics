package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Pure layout math for the laser quarry's frame: position sequence, ring traversal,
 * and per-block arm-connection state.
 *
 * <p>Construction order is bottom ring → corner pillars → top ring, matching the
 * historical {@code getNextFramePosition} sequence in the block entity.
 */
public final class FrameLayout {

    /** Number of pillar blocks placed between the bottom and top rings (4 corners × 3 high). */
    public static final int PILLAR_COUNT = 12;

    private FrameLayout() {}

    public static @Nullable BlockPos nextFramePosition(
            Direction facing, BlockPos quarryPos, QuarryBounds bounds, int defaultArea, int frameBuildIndex) {
        QuarryFrameRect rect = QuarryFrameRect.resolve(facing, quarryPos, bounds, defaultArea);
        if (rect == null) {
            return null;
        }

        int ringSize = ringSize(rect.width(), rect.depth());

        // Phase 1: bottom ring
        if (frameBuildIndex < ringSize) {
            return ringPosition(frameBuildIndex, rect.startX(), rect.startZ(), rect.endX(), rect.endZ(), rect.bottomY());
        }

        // Phase 2: corner pillars (12 = 4 corners × 3 stories)
        int pillarIndex = frameBuildIndex - ringSize;
        if (pillarIndex < PILLAR_COUNT) {
            int cornerIndex = pillarIndex / 3;
            int yOffset = (pillarIndex % 3) + 1;
            int y = rect.bottomY() + yOffset;
            return switch (cornerIndex) {
                case 0 -> new BlockPos(rect.startX(), y, rect.startZ());
                case 1 -> new BlockPos(rect.endX(), y, rect.startZ());
                case 2 -> new BlockPos(rect.endX(), y, rect.endZ());
                case 3 -> new BlockPos(rect.startX(), y, rect.endZ());
                default -> null;
            };
        }

        // Phase 3: top ring
        int topRingIndex = frameBuildIndex - ringSize - PILLAR_COUNT;
        if (topRingIndex < ringSize) {
            return ringPosition(topRingIndex, rect.startX(), rect.startZ(), rect.endX(), rect.endZ(), rect.topY());
        }

        return null;
    }

    /**
     * Every position the frame occupies, in build order. Empty when the rectangle cannot be
     * resolved. Drives frame teardown, so it must stay the exact set that
     * {@link #nextFramePosition} places.
     */
    public static List<BlockPos> framePositions(
            Direction facing, BlockPos quarryPos, QuarryBounds bounds, int defaultArea) {
        List<BlockPos> positions = new ArrayList<>();
        for (int index = 0; ; index++) {
            BlockPos pos = nextFramePosition(facing, quarryPos, bounds, defaultArea, index);
            if (pos == null) {
                return positions;
            }
            positions.add(pos);
        }
    }

    /** Perimeter count for a rectangle of the given width and depth, with corners counted once. */
    public static int ringSize(int width, int depth) {
        return 2 * width + 2 * depth - 4;
    }

    /**
     * Traverses the perimeter as: north edge (W→E), east edge (N→S), south edge (E→W), west edge (S→N).
     * Returns {@code null} if {@code index} is out of range.
     */
    public static @Nullable BlockPos ringPosition(int index, int startX, int startZ, int endX, int endZ, int y) {
        int width = endX - startX + 1;
        int depth = endZ - startZ + 1;

        if (index < width) {
            return new BlockPos(startX + index, y, startZ);
        }
        index -= width;

        int eastEdgeSize = depth - 1;
        if (index < eastEdgeSize) {
            return new BlockPos(endX, y, startZ + 1 + index);
        }
        index -= eastEdgeSize;

        int southEdgeSize = width - 1;
        if (index < southEdgeSize) {
            return new BlockPos(endX - 1 - index, y, endZ);
        }
        index -= southEdgeSize;

        int westEdgeSize = depth - 2;
        if (index < westEdgeSize) {
            return new BlockPos(startX, y, endZ - 1 - index);
        }

        return null;
    }

    /**
     * Frame block state for the given frame position, with the arm-connection
     * properties set to match the position's role (corner pillar / ring edge).
     * Returns the default block state if the rectangle cannot be resolved.
     */
    public static BlockState frameBlockState(
            Direction facing, BlockPos quarryPos, BlockPos framePos, QuarryBounds bounds, int defaultArea) {
        LaserQuarryFrameBlock frameBlock = (LaserQuarryFrameBlock) LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME;
        QuarryFrameRect rect = QuarryFrameRect.resolve(facing, quarryPos, bounds, defaultArea);
        if (rect == null) {
            return frameBlock.defaultBlockState();
        }
        boolean[] arms = computeArms(rect, framePos);
        return frameBlock.withArms(arms[0], arms[1], arms[2], arms[3], arms[4], arms[5]);
    }

    /**
     * Returns {@code {north, south, east, west, up, down}} arm-connection flags
     * for a frame block at {@code framePos} within the given rectangle. Exposes
     * the pure side of {@link #frameBlockState} without the block-registry dependency,
     * which keeps it directly unit-testable.
     */
    public static boolean[] computeArms(QuarryFrameRect rect, BlockPos framePos) {
        int x = framePos.getX();
        int y = framePos.getY();
        int z = framePos.getZ();

        boolean north = false;
        boolean south = false;
        boolean east = false;
        boolean west = false;
        boolean up = false;
        boolean down = false;

        boolean isCornerX = (x == rect.startX() || x == rect.endX());
        boolean isCornerZ = (z == rect.startZ() || z == rect.endZ());
        boolean isCorner = isCornerX && isCornerZ;

        if (isCorner) {
            if (y > rect.bottomY()) down = true;
            if (y < rect.topY()) up = true;
        }

        if (y == rect.bottomY() || y == rect.topY()) {
            if (z == rect.startZ() || z == rect.endZ()) {
                if (x > rect.startX()) west = true;
                if (x < rect.endX()) east = true;
            }
            if (x == rect.startX() || x == rect.endX()) {
                if (z > rect.startZ()) north = true;
                if (z < rect.endZ()) south = true;
            }
        }

        return new boolean[] {north, south, east, west, up, down};
    }
}
