package com.logistics.core.render;

import com.logistics.core.marker.MarkerBlockEntity;
import com.logistics.core.marker.MarkerManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.logistics.core.lib.client.render.MachineModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders laser beams for active markers.
 * Uses code-generated model segments for quad-based beams with proper thickness.
 */
public class MarkerBlockEntityRenderer implements BlockEntityRenderer<MarkerBlockEntity, MarkerRenderState> {
    public MarkerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public MarkerRenderState createRenderState() {
        return new MarkerRenderState();
    }

    @Override
    public void extractRenderState(
            MarkerBlockEntity entity,
            MarkerRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        state.active = entity.isActive();
        state.markerPos = entity.getBlockPos();
        state.connectedMarkers.clear();
        state.connectedMarkers.addAll(entity.getConnectedMarkers());
        state.boundMin = entity.getBoundMin();
        state.boundMax = entity.getBoundMax();
        state.isCornerMarker = entity.isCornerMarker();

        if (state.active && entity.getLevel() != null) {
            calculateBeamLengths(state, entity.getBlockPos());
        } else {
            state.beamNorth = 0;
            state.beamSouth = 0;
            state.beamEast = 0;
            state.beamWest = 0;
        }
    }

    private void calculateBeamLengths(MarkerRenderState state, BlockPos pos) {
        int north = 0, south = 0, east = 0, west = 0;

        if (!state.connectedMarkers.isEmpty()) {
            int posX = pos.getX();
            int posZ = pos.getZ();

            int minX = posX, maxX = posX;
            int minZ = posZ, maxZ = posZ;

            for (BlockPos connected : state.connectedMarkers) {
                minX = Math.min(minX, connected.getX());
                maxX = Math.max(maxX, connected.getX());
                minZ = Math.min(minZ, connected.getZ());
                maxZ = Math.max(maxZ, connected.getZ());
            }

            boolean hasMarkerAtNW = hasMarkerAt(state, pos, minX, minZ);
            boolean hasMarkerAtNE = hasMarkerAt(state, pos, maxX, minZ);
            boolean hasMarkerAtSW = hasMarkerAt(state, pos, minX, maxZ);

            if (posZ == minZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtNW) {
                    west = maxX - minX;
                }
            }

            if (posZ == maxZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtSW) {
                    west = maxX - minX;
                }
            }

            if (posX == minX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNW) {
                    north = maxZ - minZ;
                }
            }

            if (posX == maxX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNE) {
                    north = maxZ - minZ;
                }
            }
        } else {
            int distance = MarkerManager.MAX_MARKER_DISTANCE;
            north = distance;
            south = distance;
            east = distance;
            west = distance;
        }

        state.beamNorth = north;
        state.beamSouth = south;
        state.beamEast = east;
        state.beamWest = west;
    }

    private boolean hasMarkerAt(MarkerRenderState state, BlockPos thisPos, int x, int z) {
        if (thisPos.getX() == x && thisPos.getZ() == z) {
            return true;
        }
        for (BlockPos connected : state.connectedMarkers) {
            if (connected.getX() == x && connected.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void submit(
            MarkerRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.active) {
            return;
        }

        BlockStateModel beamModel = MachineModels.model("marker_beam");
        RenderType renderLayer = RenderTypes.cutoutMovingBlock();

        if (state.beamNorth > 0) {
            renderBeamInDirection(matrices, queue, beamModel, renderLayer, state.lightCoords, 180, state.beamNorth);
        }
        if (state.beamSouth > 0) {
            renderBeamInDirection(matrices, queue, beamModel, renderLayer, state.lightCoords, 0, state.beamSouth);
        }
        if (state.beamEast > 0) {
            renderBeamInDirection(matrices, queue, beamModel, renderLayer, state.lightCoords, 90, state.beamEast);
        }
        if (state.beamWest > 0) {
            renderBeamInDirection(matrices, queue, beamModel, renderLayer, state.lightCoords, -90, state.beamWest);
        }
    }

    private void renderBeamInDirection(
            PoseStack matrices,
            SubmitNodeCollector queue,
            BlockStateModel model,
            RenderType renderLayer,
            int lightmap,
            float yRotation,
            int length) {
        for (int i = 0; i < length; i++) {
            matrices.pushPose();
            matrices.translate(0.5, -0.0625, 0.5);
            matrices.mulPose(Axis.YP.rotationDegrees(yRotation));
            matrices.translate(0, 0, i);
            matrices.translate(-0.5, 0.0, 0.0);
            queue.submitBlockModel(
                    matrices,
                    renderLayer,
                    model,
                    1f,
                    1f,
                    1f,
                    lightmap,
                    OverlayTexture.NO_OVERLAY,
                    0);
            matrices.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    /**
     * The marker's beam and area box reach well outside the marker's own section, so culling on
     * that section alone would drop the whole thing from view. {@link #getViewDistance()} is a
     * separate gate and does not cover this.
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
