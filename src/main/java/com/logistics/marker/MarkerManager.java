package com.logistics.marker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Manages marker connections and bounding box calculations.
 */
public final class MarkerManager {
    private MarkerManager() {}

    public static final int MAX_MARKER_DISTANCE = 64;

    /**
     * Result of attempting to activate a marker.
     */
    public record ActivationResult(boolean success, String message) {}

    /**
     * Bounding box result from marker configuration.
     */
    public record MarkerBounds(BlockPos min, BlockPos max, List<BlockPos> allMarkers) {}

    /**
     * Try to activate a marker and find connected markers to form a valid configuration.
     */
    public static ActivationResult tryActivateMarker(World world, BlockPos markerPos) {
        // Find all markers along cardinal directions
        List<BlockPos> northSouth = new ArrayList<>();
        List<BlockPos> eastWest = new ArrayList<>();

        // Search in all 4 cardinal directions
        BlockPos north = findMarkerInDirection(world, markerPos, Direction.NORTH);
        BlockPos south = findMarkerInDirection(world, markerPos, Direction.SOUTH);
        BlockPos east = findMarkerInDirection(world, markerPos, Direction.EAST);
        BlockPos west = findMarkerInDirection(world, markerPos, Direction.WEST);

        if (north != null) northSouth.add(north);
        if (south != null) northSouth.add(south);
        if (east != null) eastWest.add(east);
        if (west != null) eastWest.add(west);

        // We need at least one marker in each axis direction to form an L
        if (northSouth.isEmpty() && eastWest.isEmpty()) {
            return new ActivationResult(false, "No other markers found nearby");
        }

        // Try to find a valid L-shape (3 markers forming a right angle)
        // The activated marker could be:
        // 1. The corner marker (has one connection in each perpendicular direction)
        // 2. An edge marker (need to find if there's another marker that forms the corner)

        // Case 1: This marker is the corner
        if (!northSouth.isEmpty() && !eastWest.isEmpty()) {
            BlockPos nsMarker = northSouth.get(0);
            BlockPos ewMarker = eastWest.get(0);
            return activateTriangle(world, markerPos, nsMarker, ewMarker, markerPos);
        }

        // Case 2: This marker is on an edge, need to find the corner
        // Check each connected marker to see if it can be the corner
        for (BlockPos connected : northSouth) {
            // Check if the connected marker has a perpendicular connection
            BlockPos perpendicular = findPerpendicularConnection(world, connected, Direction.NORTH, Direction.SOUTH);
            if (perpendicular != null) {
                return activateTriangle(world, markerPos, connected, perpendicular, connected);
            }
        }

        for (BlockPos connected : eastWest) {
            BlockPos perpendicular = findPerpendicularConnection(world, connected, Direction.EAST, Direction.WEST);
            if (perpendicular != null) {
                return activateTriangle(world, markerPos, connected, perpendicular, connected);
            }
        }

        return new ActivationResult(false, "Cannot form valid L-shape. Need 3 markers in right triangle.");
    }

    /**
     * Activate three markers forming a triangle/L-shape.
     * Only handles 2D rectangles (X and Z axes).
     */
    private static ActivationResult activateTriangle(World world, BlockPos marker1, BlockPos marker2, BlockPos marker3, BlockPos cornerPos) {
        // Calculate 2D bounds (X and Z only)
        int minX = Math.min(Math.min(marker1.getX(), marker2.getX()), marker3.getX());
        int maxX = Math.max(Math.max(marker1.getX(), marker2.getX()), marker3.getX());
        int minZ = Math.min(Math.min(marker1.getZ(), marker2.getZ()), marker3.getZ());
        int maxZ = Math.max(Math.max(marker1.getZ(), marker2.getZ()), marker3.getZ());

        int y = cornerPos.getY();

        BlockPos boundMin = new BlockPos(minX, y, minZ);
        BlockPos boundMax = new BlockPos(maxX, y, maxZ);

        // Collect all markers to activate (only the 3 horizontal markers)
        List<BlockPos> allMarkers = new ArrayList<>();
        allMarkers.add(marker1);
        if (!marker2.equals(marker1)) allMarkers.add(marker2);
        if (!marker3.equals(marker1) && !marker3.equals(marker2)) allMarkers.add(marker3);

        // Activate all markers
        for (BlockPos markerPos : allMarkers) {
            BlockEntity entity = world.getBlockEntity(markerPos);
            if (entity instanceof MarkerBlockEntity marker) {
                List<BlockPos> connections = new ArrayList<>(allMarkers);
                connections.remove(markerPos);
                boolean isCorner = markerPos.equals(cornerPos);
                marker.activate(connections, boundMin, boundMax, isCorner);
            }
        }

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        String sizeInfo = width + "x" + depth;
        return new ActivationResult(true, "Area defined: " + sizeInfo);
    }

    /**
     * Find a marker in the given direction from the starting position.
     */
    @Nullable private static BlockPos findMarkerInDirection(World world, BlockPos start, Direction direction) {
        BlockPos.Mutable mutable = start.mutableCopy();

        for (int i = 1; i <= MAX_MARKER_DISTANCE; i++) {
            mutable.move(direction);
            BlockState state = world.getBlockState(mutable);

            if (state.getBlock() instanceof MarkerBlock) {
                return mutable.toImmutable();
            }

            // Stop at non-air blocks that would obstruct the beam
            // (but allow transparent blocks, fluids, etc.)
            if (state.isOpaqueFullCube()) {
                break;
            }
        }

        return null;
    }

    /**
     * Find a perpendicular marker connection from the given position.
     */
    @Nullable private static BlockPos findPerpendicularConnection(World world, BlockPos pos, Direction exclude1, Direction exclude2) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (dir == exclude1 || dir == exclude2) continue;

            BlockPos found = findMarkerInDirection(world, pos, dir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }


    /**
     * Check if a position is adjacent to a valid marker-defined area.
     * Returns the bounds if found, null otherwise.
     */
    @Nullable public static MarkerBounds findAdjacentMarkerBounds(World world, BlockPos quarryPos) {
        // Check all 4 horizontal neighbors
        for (Direction dir : Direction.Type.HORIZONTAL) {
            // Look for markers in each direction along the perimeter
            MarkerBounds bounds = checkForMarkerAreaInDirection(world, quarryPos, dir);
            if (bounds != null) {
                return bounds;
            }
        }

        return null;
    }

    /**
     * Check for a valid marker area in the given direction from the quarry position.
     * Searches outward in expanding shells constrained to the half-space in the given direction.
     */
    @Nullable private static MarkerBounds checkForMarkerAreaInDirection(World world, BlockPos quarryPos, Direction dir) {
        Set<MarkerBlockEntity> checkedMarkers = new HashSet<>();

        int quarryX = quarryPos.getX();
        int quarryY = quarryPos.getY();
        int quarryZ = quarryPos.getZ();

        // Y search range - markers are typically at or near quarry level
        int minDY = -3;
        int maxDY = 3;

        // Search outward in expanding shells in the given direction
        for (int distance = 1; distance <= MAX_MARKER_DISTANCE; distance++) {
            // For each distance, check positions on the face perpendicular to the search direction
            // Also check positions along the sides that extend into this direction's half-space

            for (int dy = minDY; dy <= maxDY; dy++) {
                int y = quarryY + dy;

                switch (dir) {
                    case NORTH -> {
                        // Search face at z = quarryZ - distance, x from -distance to +distance
                        int z = quarryZ - distance;
                        for (int dx = -distance; dx <= distance; dx++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX + dx, y, z, checkedMarkers);
                            if (result != null) return result;
                        }
                        // Also check side edges at this distance (x = quarryX +/- distance, z from -(distance-1) to -1)
                        for (int dz = 1; dz < distance; dz++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX - distance, y, quarryZ - dz, checkedMarkers);
                            if (result != null) return result;
                            result = checkMarkerPosition(world, quarryPos, quarryX + distance, y, quarryZ - dz, checkedMarkers);
                            if (result != null) return result;
                        }
                    }
                    case SOUTH -> {
                        // Search face at z = quarryZ + distance
                        int z = quarryZ + distance;
                        for (int dx = -distance; dx <= distance; dx++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX + dx, y, z, checkedMarkers);
                            if (result != null) return result;
                        }
                        // Side edges
                        for (int dz = 1; dz < distance; dz++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX - distance, y, quarryZ + dz, checkedMarkers);
                            if (result != null) return result;
                            result = checkMarkerPosition(world, quarryPos, quarryX + distance, y, quarryZ + dz, checkedMarkers);
                            if (result != null) return result;
                        }
                    }
                    case EAST -> {
                        // Search face at x = quarryX + distance
                        int x = quarryX + distance;
                        for (int dz = -distance; dz <= distance; dz++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, x, y, quarryZ + dz, checkedMarkers);
                            if (result != null) return result;
                        }
                        // Side edges
                        for (int dx = 1; dx < distance; dx++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX + dx, y, quarryZ - distance, checkedMarkers);
                            if (result != null) return result;
                            result = checkMarkerPosition(world, quarryPos, quarryX + dx, y, quarryZ + distance, checkedMarkers);
                            if (result != null) return result;
                        }
                    }
                    case WEST -> {
                        // Search face at x = quarryX - distance
                        int x = quarryX - distance;
                        for (int dz = -distance; dz <= distance; dz++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, x, y, quarryZ + dz, checkedMarkers);
                            if (result != null) return result;
                        }
                        // Side edges
                        for (int dx = 1; dx < distance; dx++) {
                            MarkerBounds result = checkMarkerPosition(world, quarryPos, quarryX - dx, y, quarryZ - distance, checkedMarkers);
                            if (result != null) return result;
                            result = checkMarkerPosition(world, quarryPos, quarryX - dx, y, quarryZ + distance, checkedMarkers);
                            if (result != null) return result;
                        }
                    }
                    default -> { /* UP/DOWN not used for horizontal marker search */ }
                }
            }
        }

        return null;
    }

    /**
     * Check a single position for a valid marker that defines bounds adjacent to the quarry.
     */
    @Nullable private static MarkerBounds checkMarkerPosition(World world, BlockPos quarryPos, int x, int y, int z, Set<MarkerBlockEntity> checkedMarkers) {
        BlockPos checkPos = new BlockPos(x, y, z);
        BlockEntity entity = world.getBlockEntity(checkPos);

        if (entity instanceof MarkerBlockEntity marker && marker.isActive() && marker.hasValidBounds()) {
            if (checkedMarkers.contains(marker)) return null;
            checkedMarkers.add(marker);

            BlockPos min = marker.getBoundMin();
            BlockPos max = marker.getBoundMax();

            if (isAdjacentToBounds(quarryPos, min, max)) {
                List<BlockPos> allMarkers = new ArrayList<>();
                allMarkers.add(checkPos);
                allMarkers.addAll(marker.getConnectedMarkers());
                return new MarkerBounds(min, max, allMarkers);
            }
        }

        return null;
    }

    /**
     * Check if a position is adjacent (exactly 1 block outside) the bounds.
     */
    private static boolean isAdjacentToBounds(BlockPos pos, BlockPos min, BlockPos max) {
        int x = pos.getX();
        int z = pos.getZ();
        int y = pos.getY();

        int minX = min.getX();
        int maxX = max.getX();
        int minZ = min.getZ();
        int maxZ = max.getZ();
        int minY = min.getY();
        int maxY = max.getY();

        // Check if Y is within reasonable range (at marker level or slightly above/below)
        if (y < minY - 1 || y > minY + 1) {
            return false;
        }

        // Check if position is exactly 1 block outside the bounds on one side
        // and within bounds on the other axis
        boolean adjacentX = (x == minX - 1 || x == maxX + 1) && z >= minZ && z <= maxZ;
        boolean adjacentZ = (z == minZ - 1 || z == maxZ + 1) && x >= minX && x <= maxX;

        return adjacentX || adjacentZ;
    }

    /**
     * Break and drop all markers in the given list.
     */
    public static void breakMarkers(World world, List<BlockPos> markerPositions) {
        for (BlockPos pos : markerPositions) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof MarkerBlock) {
                // Drop the marker as an item
                Block.dropStacks(state, world, pos);
                // Remove the block
                world.removeBlock(pos, false);
            }
        }
    }
}
