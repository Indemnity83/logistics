package com.logistics.core.render;

import com.logistics.core.marker.MarkerBlockEntity;
import com.logistics.core.marker.MarkerManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders laser beams for active markers in MC 1.21.1.
 * TODO: Improve beam rendering to match MC 1.21.11 version (quad-based beams with proper thickness)
 * Currently uses simple line rendering for compatibility.
 */
public class MarkerBlockEntityRenderer implements BlockEntityRenderer<MarkerBlockEntity> {
    // Beam color (bright blue: #0132FD)
    private static final float BEAM_RED = 0.2f;
    private static final float BEAM_GREEN = 0.5f;
    private static final float BEAM_BLUE = 1.0f;
    private static final float BEAM_ALPHA = 1.0f;

    public MarkerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            MarkerBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (!entity.isActive()) {
            return;
        }

        // Calculate which beams to render
        BeamLengths beams = calculateBeamLengths(entity);

        // Render beams as simple blue lines
        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.3, 0.5); // Center of marker

        Matrix4f matrix = poseStack.last().pose();

        if (beams.north > 0) {
            lineBuffer.addVertex(matrix, 0, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
            lineBuffer.addVertex(matrix, 0, 0, -beams.north).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
        }
        if (beams.south > 0) {
            lineBuffer.addVertex(matrix, 0, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
            lineBuffer.addVertex(matrix, 0, 0, beams.south).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
        }
        if (beams.east > 0) {
            lineBuffer.addVertex(matrix, 0, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
            lineBuffer.addVertex(matrix, beams.east, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
        }
        if (beams.west > 0) {
            lineBuffer.addVertex(matrix, 0, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
            lineBuffer.addVertex(matrix, -beams.west, 0, 0).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA).setNormal(0, 1, 0);
        }

        poseStack.popPose();
    }

    private BeamLengths calculateBeamLengths(MarkerBlockEntity entity) {
        List<BlockPos> connectedMarkers = entity.getConnectedMarkers();
        BlockPos pos = entity.getBlockPos();

        if (!connectedMarkers.isEmpty()) {
            // Connected mode - draw beams to form rectangle outline
            int north = 0, south = 0, east = 0, west = 0;

            int posX = pos.getX();
            int posZ = pos.getZ();

            // Compute the rectangle bounds from this marker + connected markers
            int minX = posX;
            int maxX = posX;
            int minZ = posZ;
            int maxZ = posZ;

            for (BlockPos connected : connectedMarkers) {
                minX = Math.min(minX, connected.getX());
                maxX = Math.max(maxX, connected.getX());
                minZ = Math.min(minZ, connected.getZ());
                maxZ = Math.max(maxZ, connected.getZ());
            }

            // Check which corners have markers
            boolean hasMarkerAtNW = hasMarkerAt(entity, pos, minX, minZ);
            boolean hasMarkerAtNE = hasMarkerAt(entity, pos, maxX, minZ);
            boolean hasMarkerAtSW = hasMarkerAt(entity, pos, minX, maxZ);

            // North edge (z = minZ): from (minX, minZ) to (maxX, minZ)
            if (posZ == minZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtNW) {
                    west = maxX - minX;
                }
            }

            // South edge (z = maxZ): from (minX, maxZ) to (maxX, maxZ)
            if (posZ == maxZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtSW) {
                    west = maxX - minX;
                }
            }

            // West edge (x = minX): from (minX, minZ) to (minX, maxZ)
            if (posX == minX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNW) {
                    north = maxZ - minZ;
                }
            }

            // East edge (x = maxX): from (maxX, minZ) to (maxX, maxZ)
            if (posX == maxX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNE) {
                    north = maxZ - minZ;
                }
            }

            return new BeamLengths(north, south, east, west);
        } else {
            // Solo mode - project beams in all directions
            int distance = MarkerManager.MAX_MARKER_DISTANCE;
            return new BeamLengths(distance, distance, distance, distance);
        }
    }

    private boolean hasMarkerAt(MarkerBlockEntity entity, BlockPos thisPos, int x, int z) {
        if (thisPos.getX() == x && thisPos.getZ() == z) {
            return true;
        }
        for (BlockPos connected : entity.getConnectedMarkers()) {
            if (connected.getX() == x && connected.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }

    private record BeamLengths(int north, int south, int east, int west) {}
}
