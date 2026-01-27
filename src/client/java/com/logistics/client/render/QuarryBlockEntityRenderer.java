package com.logistics.client.render;

import com.logistics.LogisticsMod;
import com.logistics.quarry.QuarryBlock;
import com.logistics.quarry.QuarryConfig;
import com.logistics.quarry.entity.QuarryBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the quarry arm visualization.
 * Shows horizontal beams on top of the frame and a vertical drill arm
 * that moves smoothly to the current mining position.
 */
public class QuarryBlockEntityRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryRenderState> {
    private static final Identifier ARM_MODEL_ID = Identifier.of(LogisticsMod.MOD_ID, "block/quarry_gantry_arm");
    private static final Identifier DRILL_MODEL_ID = Identifier.of(LogisticsMod.MOD_ID, "block/quarry_drill");

    public QuarryBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public QuarryRenderState createRenderState() {
        return new QuarryRenderState();
    }

    @Override
    public void updateRenderState(
            QuarryBlockEntity entity,
            QuarryRenderState state,
            float tickDelta,
            Vec3d cameraPos,
            @Nullable net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        net.minecraft.client.render.block.entity.state.BlockEntityRenderState.updateBlockEntityRenderState(
                entity, state, crumblingOverlay);

        state.quarryPos = entity.getPos();
        state.phase = entity.getCurrentPhase();
        state.armState = entity.getArmState();

        // Only render arm during mining phase when arm is initialized
        state.shouldRenderArm = (state.phase == QuarryBlockEntity.Phase.MINING) && entity.isArmInitialized();

        if (!state.shouldRenderArm) {
            return;
        }

        World world = entity.getWorld();
        if (world == null) {
            state.shouldRenderArm = false;
            return;
        }

        // Check if the block is still a quarry (could be removed/replaced)
        BlockState blockState = world.getBlockState(state.quarryPos);
        if (!(blockState.getBlock() instanceof QuarryBlock)) {
            state.shouldRenderArm = false;
            return;
        }

        // Get facing direction
        state.facing = QuarryBlock.getMiningDirection(blockState);

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
                    state.frameStartZ = quarryPos.getZ() - QuarryConfig.CHUNK_SIZE;
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
                    state.frameStartX = quarryPos.getX() - QuarryConfig.CHUNK_SIZE;
                    state.frameStartZ = quarryPos.getZ() - 8;
                    break;
                default:
                    state.shouldRenderArm = false;
                    return;
            }
            state.frameEndX = state.frameStartX + QuarryConfig.CHUNK_SIZE - 1;
            state.frameEndZ = state.frameStartZ + QuarryConfig.CHUNK_SIZE - 1;
        }
        state.frameTopY = quarryPos.getY() + QuarryConfig.Y_OFFSET_ABOVE;

        // Sample light at the frame top level (where the horizontal beams are)
        BlockPos frameTopPos = new BlockPos(
                (state.frameStartX + state.frameEndX) / 2, state.frameTopY, (state.frameStartZ + state.frameEndZ) / 2);
        state.frameTopLight = net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(world, frameTopPos);

        // Get server-synced arm position (interpolation happens in render() for smooth frame-rate independent movement)
        state.serverArmX = entity.getArmX();
        state.serverArmY = entity.getArmY();
        state.serverArmZ = entity.getArmZ();
    }

    @Override
    public void render(
            QuarryRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {
        if (!state.shouldRenderArm) {
            return;
        }

        BlockStateModel armModel = PipeModelRegistry.getModel(ARM_MODEL_ID);
        if (armModel == null) {
            return;
        }

        // Update interpolation every frame for smooth movement
        state.updateClientInterpolation();

        RenderLayer renderLayer = RenderLayers.cutout();

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
        BlockStateModel drillModel = PipeModelRegistry.getModel(DRILL_MODEL_ID);
        if (drillModel != null) {
            matrices.push();
            // Position drill at arm location, offset to center the model
            // Drill model is centered at X=0.5, Z=0.5, extends from Y=0.125 to Y=1
            matrices.translate(relArmX - 0.5, relArmY, relArmZ - 0.5);
            queue.submitBlockStateModel(
                    matrices, renderLayer, drillModel, 1.0f, 1.0f, 1.0f, light, OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
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
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            BlockStateModel model,
            RenderLayer renderLayer,
            int lightmap,
            float startX,
            float startY,
            float startZ,
            int length,
            boolean alongX) {
        for (int i = 0; i < length; i++) {
            matrices.push();

            if (alongX) {
                // Beam extends along X axis (east-west)
                // Position at segment, centered on startZ
                matrices.translate(startX + i + 0.5, startY + 0.5, startZ);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90)); // Point east
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

            queue.submitBlockStateModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.DEFAULT_UV, 0);

            matrices.pop();
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
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            BlockStateModel model,
            RenderLayer renderLayer,
            int lightmap,
            float x,
            float startY,
            float z,
            float length) {
        int fullSegments = (int) length;
        float remainder = length - fullSegments;

        // Render partial segment at the TOP first (obscured by horizontal beams)
        if (remainder > 0.1f) {
            matrices.push();

            // Partial segment starts at startY and grows downward
            matrices.translate(x, startY, z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            // Scale the partial segment in Z (which is now -Y after rotation)
            matrices.scale(1.0f, 1.0f, remainder);
            matrices.translate(-0.5, -0.5, 0.0);

            queue.submitBlockStateModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.DEFAULT_UV, 0);

            matrices.pop();
        }

        // Render full block segments below the partial segment
        for (int i = 0; i < fullSegments; i++) {
            matrices.push();

            // Full segments start below the partial segment
            matrices.translate(x, startY - remainder - i, z);
            // Rotate to point downward
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrices.translate(-0.5, -0.5, 0.0);

            queue.submitBlockStateModel(
                    matrices, renderLayer, model, 1.0f, 1.0f, 1.0f, lightmap, OverlayTexture.DEFAULT_UV, 0);

            matrices.pop();
        }
    }

    @Override
    public int getRenderDistance() {
        return 256; // Visible from far away
    }
}
