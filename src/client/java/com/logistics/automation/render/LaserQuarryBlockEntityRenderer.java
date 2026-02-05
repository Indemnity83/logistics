package com.logistics.automation.render;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryConfig;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.render.ModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the quarry arm visualization.
 * Shows horizontal beams on top of the frame and a vertical drill arm
 * that moves smoothly to the current mining position.
 */
public class LaserQuarryBlockEntityRenderer implements BlockEntityRenderer<LaserQuarryBlockEntity, LaserQuarryRenderState> {
    private static final Identifier ARM_MODEL_ID =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/automation/laser_quarry_gantry_arm");
    private static final Identifier DRILL_MODEL_ID =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/automation/laser_quarry_drill");

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

        // Only render arm during mining phase when arm is initialized
        state.shouldRenderArm = (state.phase == LaserQuarryBlockEntity.Phase.MINING) && entity.isArmInitialized();

        if (!state.shouldRenderArm) {
            return;
        }

        Level level = entity.getLevel();
        if (level == null) {
            state.shouldRenderArm = false;
            return;
        }

        // Check if the block is still a quarry (could be removed/replaced)
        BlockState blockState = level.getBlockState(state.quarryPos);
        if (!(blockState.getBlock() instanceof LaserQuarryBlock)) {
            state.shouldRenderArm = false;
            return;
        }

        // Get facing direction
        state.facing = LaserQuarryBlock.getMiningDirection(blockState);

        // Calculate frame bounds - use custom bounds if available, otherwise calculate from facing
        BlockPos quarryPos = state.quarryPos;
        if (entity.hasCustomBounds()) {
            state.frameStartX = entity.getCustomMinX();
            state.frameStartZ = entity.getCustomMinZ();
            state.frameEndX = entity.getCustomMaxX();
            state.frameEndZ = entity.getCustomMaxZ();
        } else {
            switch (state.facing) {
                case NORTH:
                    state.frameStartX = quarryPos.getX() - 8;
                    state.frameStartZ = quarryPos.getZ() - LaserQuarryConfig.CHUNK_SIZE;
                    break;
                case SOUTH:
                    state.frameStartX = quarryPos.getX() - 8;
                    state.frameStartZ = quarryPos.getZ() + 1;
                    break;
                case EAST:
                    state.frameStartX = quarryPos.getX() + 1;
                    state.frameStartZ = quarryPos.getZ() - 8;
                    break;
                case WEST:
                    state.frameStartX = quarryPos.getX() - LaserQuarryConfig.CHUNK_SIZE;
                    state.frameStartZ = quarryPos.getZ() - 8;
                    break;
                default:
                    state.shouldRenderArm = false;
                    return;
            }
            state.frameEndX = state.frameStartX + LaserQuarryConfig.CHUNK_SIZE - 1;
            state.frameEndZ = state.frameStartZ + LaserQuarryConfig.CHUNK_SIZE - 1;
        }
        state.frameTopY = quarryPos.getY() + LaserQuarryConfig.Y_OFFSET_ABOVE;

        // Sample light at the center of the frame top for more accurate lighting
        int centerX = (state.frameStartX + state.frameEndX) / 2;
        int centerZ = (state.frameStartZ + state.frameEndZ) / 2;
        BlockPos frameTopPos = new BlockPos(centerX, state.frameTopY, centerZ);
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, frameTopPos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, frameTopPos);
        state.frameTopLight = LightTexture.pack(blockLight, skyLight);

        // Get server-synced arm position (interpolation happens in render() for smooth frame-rate independent movement)
        state.serverArmX = entity.getArmX();
        state.serverArmY = entity.getArmY();
        state.serverArmZ = entity.getArmZ();
    }

    @Override
    public void submit(
            LaserQuarryRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.shouldRenderArm) {
            return;
        }

        BlockStateModel armModel = ModelRegistry.getModel(ARM_MODEL_ID);
        if (armModel == null) {
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
                armModel,
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
                armModel,
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
                    matrices, queue, armModel, renderLayer, light, relArmX, verticalStartY, relArmZ, verticalLength);
        }

        // Render drill head at the bottom of the vertical beam
        BlockStateModel drillModel = ModelRegistry.getModel(DRILL_MODEL_ID);
        if (drillModel != null) {
            matrices.pushPose();
            // Position drill at arm location, offset to center the model
            // Drill model is centered at X=0.5, Z=0.5, extends from Y=0.125 to Y=1
            matrices.translate(relArmX - 0.5, relArmY, relArmZ - 0.5);
            queue.submitBlockModel(
                    matrices, renderLayer, drillModel, 1.0f, 1.0f, 1.0f, light, OverlayTexture.NO_OVERLAY, 0);
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
            BlockStateModel model,
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

            queue.submitBlockModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.NO_OVERLAY, 0);

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
            BlockStateModel model,
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

            queue.submitBlockModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.NO_OVERLAY, 0);

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

            queue.submitBlockModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.NO_OVERLAY, 0);

            matrices.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }
}
