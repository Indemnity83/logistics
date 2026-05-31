package com.logistics.automation.laserquarry.entity;

import com.logistics.automation.laserquarry.LaserQuarryGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Resolved horizontal frame rectangle of a laser quarry, including its bottom and top
 * Y coordinates. Encapsulates the facing-aware bounds math so {@link FrameLayout}
 * and {@link GridScanner} can stay focused on layout/scan logic.
 */
public record QuarryFrameRect(int startX, int startZ, int endX, int endZ, int bottomY, int topY) {

    public int width() {
        return endX - startX + 1;
    }

    public int depth() {
        return endZ - startZ + 1;
    }

    public int innerWidth() {
        return width() - 2;
    }

    public int innerDepth() {
        return depth() - 2;
    }

    public static @Nullable QuarryFrameRect resolve(
            Direction facing, BlockPos quarryPos, QuarryBounds bounds, int defaultArea) {
        int startX;
        int startZ;
        int endX;
        int endZ;
        if (bounds.isCustom()) {
            startX = bounds.getMinX();
            startZ = bounds.getMinZ();
            endX = bounds.getMaxX();
            endZ = bounds.getMaxZ();
        } else {
            int half = defaultArea / 2;
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - half;
                    startZ = quarryPos.getZ() - defaultArea;
                    break;
                case SOUTH:
                    startX = quarryPos.getX() - half;
                    startZ = quarryPos.getZ() + 1;
                    break;
                case EAST:
                    startX = quarryPos.getX() + 1;
                    startZ = quarryPos.getZ() - half;
                    break;
                case WEST:
                    startX = quarryPos.getX() - defaultArea;
                    startZ = quarryPos.getZ() - half;
                    break;
                default:
                    return null;
            }
            endX = startX + defaultArea - 1;
            endZ = startZ + defaultArea - 1;
        }

        int bottomY = quarryPos.getY();
        int topY = quarryPos.getY() + LaserQuarryGeometry.Y_OFFSET_ABOVE;
        return new QuarryFrameRect(startX, startZ, endX, endZ, bottomY, topY);
    }
}
