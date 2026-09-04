package com.logistics.automation.render;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;

import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryGeometry;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.laserquarry.entity.QuarryPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.logistics.core.lib.client.render.MachineModels;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the quarry arm visualization.
 * Shows horizontal beams on top of the frame and a vertical drill arm
 * that moves smoothly to the current mining position.
 */
public class LaserQuarryBlockEntityRenderer implements BlockEntityRenderer<LaserQuarryBlockEntity, LaserQuarryRenderState> {
    public LaserQuarryBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public LaserQuarryRenderState createRenderState() {
        return new LaserQuarryRenderState();
    }

    @Override
    public void extractRenderState(
            LaserQuarryBlockEntity entity,
            LaserQuarryRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        state.quarryPos = entity.getBlockPos();
        state.phase = entity.getCurrentPhase();
        state.armState = entity.getArmState();
        state.syncedArmSpeed = entity.getSyncedArmSpeed();

        Level level = entity.getLevel();
        if (level == null) {
            state.shouldRenderArm = false;
            state.shouldRenderPreviewOutline = false;
            return;
        }

        // Check if the block is still a quarry (could be removed/replaced)
        BlockState blockState = level.getBlockState(state.quarryPos);
        if (!(blockState.getBlock() instanceof LaserQuarryBlock)) {
            state.shouldRenderArm = false;
            state.shouldRenderPreviewOutline = false;
            return;
        }

        // Get facing direction for arm rendering
        state.facing = LaserQuarryBlock.getMiningDirection(blockState);

        // Only render arm during mining phase when arm is initialized
        state.shouldRenderArm = (state.phase == QuarryPhase.MINING) && entity.isArmInitialized();

        // Calculate frame bounds - used for arm rendering and preview outline
        // Must be computed before the early return so the preview outline has access to bounds
        BlockPos quarryPos = state.quarryPos;
        if (entity.hasCustomBounds()) {
            state.frameStartX = entity.getCustomMinX();
            state.frameStartZ = entity.getCustomMinZ();
            state.frameEndX = entity.getCustomMaxX();
            state.frameEndZ = entity.getCustomMaxZ();
        } else {
            int half = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) / 2;
            switch (state.facing) {
                case NORTH:
                    state.frameStartX = quarryPos.getX() - half;
                    state.frameStartZ = quarryPos.getZ() - LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);
                    break;
                case SOUTH:
                    state.frameStartX = quarryPos.getX() - half;
                    state.frameStartZ = quarryPos.getZ() + 1;
                    break;
                case EAST:
                    state.frameStartX = quarryPos.getX() + 1;
                    state.frameStartZ = quarryPos.getZ() - half;
                    break;
                case WEST:
                    state.frameStartX = quarryPos.getX() - LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);
                    state.frameStartZ = quarryPos.getZ() - half;
                    break;
                default:
                    state.shouldRenderArm = false;
                    state.shouldRenderPreviewOutline = false;
                    return;
            }
            state.frameEndX = state.frameStartX + LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 1;
            state.frameEndZ = state.frameStartZ + LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 1;
        }
        state.frameTopY = quarryPos.getY() + LaserQuarryGeometry.Y_OFFSET_ABOVE;

        state.shouldRenderPreviewOutline = entity.isFreshlyPlaced();

        if (!state.shouldRenderArm) {
            return;
        }

        // Sample light at the center of the frame top for more accurate lighting
        int centerX = (state.frameStartX + state.frameEndX) / 2;
        int centerZ = (state.frameStartZ + state.frameEndZ) / 2;
        BlockPos frameTopPos = new BlockPos(centerX, state.frameTopY, centerZ);
        int blockLight = level.getBrightness(LightLayer.BLOCK, frameTopPos);
        int skyLight = level.getBrightness(LightLayer.SKY, frameTopPos);
        state.frameTopLight = LightCoordsUtil.pack(blockLight, skyLight);

        // Get server-synced arm position (interpolation happens in render() for smooth frame-rate independent movement)
        state.serverArmX = entity.getArmX();
        state.serverArmY = entity.getArmY();
        state.serverArmZ = entity.getArmZ();
    }

    @Override
    public void submit(
            LaserQuarryRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (state.shouldRenderPreviewOutline) {
            renderFramePreviewOutline(state, matrices, queue);
        }

        if (!state.shouldRenderArm) {
            return;
        }

        List<BlockStateModelPart> armParts = MachineModels.parts("laser_quarry_gantry_arm");
        if (armParts.isEmpty()) {
            return;
        }

        // Update interpolation every frame for smooth movement
        state.updateClientInterpolation();

        RenderType renderLayer = RenderTypes.cutoutMovingBlock();

        // Calculate positions relative to the quarry block (render origin)
        float quarryX = state.quarryPos.getX();
        float quarryY = state.quarryPos.getY();
        float quarryZ = state.quarryPos.getZ();

        // Use client-interpolated arm position (already includes +0.5 for centering)
        float relArmX = state.renderArmX - quarryX;
        float relArmZ = state.renderArmZ - quarryZ;
        float relArmY = state.renderArmY - quarryY;
        float relFrameTopY = state.frameTopY - quarryY;

        // Use light level from frame top (where horizontal beams connect)
        int light = state.frameTopLight;

        // Calculate beam lengths from actual frame bounds
        int beamLengthX = state.frameEndX - state.frameStartX; // Width of frame minus 1 (inside frame)
        int beamLengthZ = state.frameEndZ - state.frameStartZ; // Depth of frame minus 1 (inside frame)

        // East-West beam: at armZ, spanning inside the frame (not overlapping frame blocks)
        renderHorizontalBeam(
                matrices,
                queue,
                armParts,
                renderLayer,
                light,
                state.frameStartX + 1 - quarryX,
                relFrameTopY,
                relArmZ,
                beamLengthX,
                true); // true = along X axis

        // North-South beam: at armX, spanning inside the frame
        renderHorizontalBeam(
                matrices,
                queue,
                armParts,
                renderLayer,
                light,
                relArmX,
                relFrameTopY,
                state.frameStartZ + 1 - quarryZ,
                beamLengthZ,
                false); // false = along Z axis

        // Vertical drill beam: starts 0.5 above frameTopY to connect with horizontal beams
        float verticalStartY = relFrameTopY + 0.75f;
        float verticalLength = verticalStartY - relArmY - 1;
        if (verticalLength > 0.1f) {
            renderVerticalBeam(
                    matrices, queue, armParts, renderLayer, light, relArmX, verticalStartY, relArmZ, verticalLength);
        }

        // Render drill head at the bottom of the vertical beam
        List<BlockStateModelPart> drillParts = MachineModels.parts("laser_quarry_drill");
        if (!drillParts.isEmpty()) {
            matrices.pushPose();
            // Position drill at arm location, offset to center the model
            // Drill model is centered at X=0.5, Z=0.5, extends from Y=0.125 to Y=1
            matrices.translate(relArmX - 0.5, relArmY, relArmZ - 0.5);
            queue.submitBlockModel(matrices, renderLayer, drillParts, new int[]{0xFFFFFF}, light, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }

    /**
     * Render a horizontal beam.
     * Model (quarry_gantry_arm) is centered at X=0.5, Y=0.5, extends in +Z from 0 to 1.
     * @param alongX if true, beam extends along X axis; if false, along Z axis
     * @param startX for alongX: block-aligned start X; for !alongX: centered arm X position
     * @param startZ for alongX: centered arm Z position; for !alongX: block-aligned start Z
     */
    private void renderHorizontalBeam(
            PoseStack matrices,
            SubmitNodeCollector queue,
            List<BlockStateModelPart> modelParts,
            RenderType renderLayer,
            int lightmap,
            float startX,
            float startY,
            float startZ,
            int length,
            boolean alongX) {
        for (int i = 0; i < length; i++) {
            matrices.pushPose();

            if (alongX) {
                // Beam extends along X axis (east-west)
                // Position at segment, centered on startZ
                matrices.translate(startX + i + 0.5, startY + 0.5, startZ);
                matrices.mulPose(Axis.YP.rotationDegrees(-90)); // Point east
                // Center the model (model is at X=0.5, Y=0.5)
                matrices.translate(-0.5, -0.5, 0.0);
            } else {
                // Beam extends along Z axis (north-south)
                // Position at segment, centered on startX
                matrices.translate(startX, startY + 0.5, startZ + i);
                // No rotation needed, model extends in +Z
                // Center the model
                matrices.translate(-0.5, -0.5, -0.5);
            }

            queue.submitBlockModel(matrices, renderLayer, modelParts, new int[]{0xFFFFFF}, lightmap, OverlayTexture.NO_OVERLAY, 0);

            matrices.popPose();
        }
    }

    /**
     * Render a vertical beam going downward with smooth length.
     * Model is centered at X=0.5, Y=0.5, extends in +Z from 0 to 1.
     * After rotating 90° around X, it extends in -Y (downward).
     * Growth happens from the top (partial segment at top, obscured by horizontal beams).
     * @param x centered X position (already includes +0.5 offset)
     * @param z centered Z position (already includes +0.5 offset)
     */
    private void renderVerticalBeam(
            PoseStack matrices,
            SubmitNodeCollector queue,
            List<BlockStateModelPart> modelParts,
            RenderType renderLayer,
            int lightmap,
            float x,
            float startY,
            float z,
            float length) {
        int fullSegments = (int) length;
        float remainder = length - fullSegments;

        // Render partial segment at the TOP first (obscured by horizontal beams)
        if (remainder > 0.1f) {
            matrices.pushPose();

            // Partial segment starts at startY and grows downward
            matrices.translate(x, startY, z);
            matrices.mulPose(Axis.XP.rotationDegrees(90));
            // Scale the partial segment in Z (which is now -Y after rotation)
            matrices.scale(1.0f, 1.0f, remainder);
            matrices.translate(-0.5, -0.5, 0.0);

            queue.submitBlockModel(matrices, renderLayer, modelParts, new int[]{0xFFFFFF}, lightmap, OverlayTexture.NO_OVERLAY, 0);

            matrices.popPose();
        }

        // Render full block segments below the partial segment
        for (int i = 0; i < fullSegments; i++) {
            matrices.pushPose();

            // Full segments start below the partial segment
            matrices.translate(x, startY - remainder - i, z);
            // Rotate to point downward
            matrices.mulPose(Axis.XP.rotationDegrees(90));
            matrices.translate(-0.5, -0.5, 0.0);

            queue.submitBlockModel(matrices, renderLayer, modelParts, new int[]{0xFFFFFF}, lightmap, OverlayTexture.NO_OVERLAY, 0);

            matrices.popPose();
        }
    }

    private void renderFramePreviewOutline(
            LaserQuarryRenderState state, PoseStack matrices, SubmitNodeCollector queue) {
        List<BlockStateModelPart> parts = MachineModels.parts("construction_beam");
        if (parts.isEmpty()) {
            return;
        }

        RenderType renderLayer = RenderTypes.cutoutMovingBlock();
        int lightmap = LightCoordsUtil.FULL_BRIGHT;

        float qx = state.quarryPos.getX();
        float qz = state.quarryPos.getZ();

        float relStartX = state.frameStartX - qx;
        float relEndX = state.frameEndX - qx;
        float relStartZ = state.frameStartZ - qz;
        float relEndZ = state.frameEndZ - qz;

        int width = state.frameEndX - state.frameStartX;
        int depth = state.frameEndZ - state.frameStartZ;
        int height = LaserQuarryGeometry.Y_OFFSET_ABOVE;

        // Bottom ring (at quarry Y level, relY = 0)
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, 0, relStartZ, width, 90);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, 0, relEndZ, width, 90);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, 0, relStartZ, depth, 0);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relEndX, 0, relStartZ, depth, 0);

        // Top ring (at quarry Y + Y_OFFSET_ABOVE)
        float topY = LaserQuarryGeometry.Y_OFFSET_ABOVE;
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, topY, relStartZ, width, 90);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, topY, relEndZ, width, 90);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, topY, relStartZ, depth, 0);
        renderOutlineHorizontalEdge(matrices, queue, parts, renderLayer, lightmap, relEndX, topY, relStartZ, depth, 0);

        // Vertical corner edges
        renderOutlineVerticalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, 0, relStartZ, height);
        renderOutlineVerticalEdge(matrices, queue, parts, renderLayer, lightmap, relEndX, 0, relStartZ, height);
        renderOutlineVerticalEdge(matrices, queue, parts, renderLayer, lightmap, relStartX, 0, relEndZ, height);
        renderOutlineVerticalEdge(matrices, queue, parts, renderLayer, lightmap, relEndX, 0, relEndZ, height);
    }

    private void renderOutlineHorizontalEdge(
            PoseStack matrices,
            SubmitNodeCollector queue,
            List<BlockStateModelPart> parts,
            RenderType renderLayer,
            int lightmap,
            float relX,
            float relY,
            float relZ,
            int length,
            float yRotation) {
        for (int i = 0; i < length; i++) {
            matrices.pushPose();
            matrices.translate(relX + 0.5, relY - 0.0625, relZ + 0.5);
            matrices.mulPose(Axis.YP.rotationDegrees(yRotation));
            matrices.translate(0, 0, i);
            matrices.translate(-0.5, 0, 0);
            queue.submitBlockModel(
                    matrices, renderLayer, parts, new int[]{0xFFFFFF}, lightmap, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }

    private void renderOutlineVerticalEdge(
            PoseStack matrices,
            SubmitNodeCollector queue,
            List<BlockStateModelPart> parts,
            RenderType renderLayer,
            int lightmap,
            float relX,
            float relStartY,
            float relZ,
            int height) {
        // After Axis.XP.rotationDegrees(-90): local X→world X, local Z→world +Y, local Y→world -Z.
        // translate(-0.5, 0, 0) centers in world X.
        // Model Y center is 9.0/16 = 0.5625 (not 0.5), so Z translation uses relZ+1.0625 to center
        // the beam cross-section at relZ+0.5 (world Z = translationZ - modelYCenter = relZ+1.0625 - 0.5625).
        // Renders height segments offset by +0.5 so the column spans Y=relStartY+0.5 to relStartY+height+0.5,
        // matching the horizontal ring beam centers (bottom ring centered at +0.5, top ring at +height+0.5).
        for (int i = 0; i < height; i++) {
            matrices.pushPose();
            matrices.translate(relX + 0.5, relStartY + 0.5 + i, relZ + 1.0625);
            matrices.mulPose(Axis.XP.rotationDegrees(-90));
            matrices.translate(-0.5, 0, 0);
            queue.submitBlockModel(
                    matrices, renderLayer, parts, new int[]{0xFFFFFF}, lightmap, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }

    /**
     * The frame and laser reach well outside the quarry's own section, so culling on
     * that section alone would drop the whole thing from view. {@link #getViewDistance()} is a
     * separate gate and does not cover this.
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
