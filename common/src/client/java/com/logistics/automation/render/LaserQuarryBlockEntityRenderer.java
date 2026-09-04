package com.logistics.automation.render;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryGeometry;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.laserquarry.entity.QuarryPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.logistics.core.lib.client.render.CodeModelRenderer;
import com.logistics.core.lib.client.render.MachineModels;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders the quarry arm visualization.
 * Shows horizontal beams on top of the frame and a vertical drill arm
 * that moves smoothly to the current mining position.
 */
public class LaserQuarryBlockEntityRenderer implements BlockEntityRenderer<LaserQuarryBlockEntity> {
    // Persistent interpolation state stored per quarry position (survives render calls)
    private static final Map<BlockPos, InterpolationState> INTERPOLATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, LedFadeState> LED_FADE_CACHE = new ConcurrentHashMap<>();

    // Green LED fade duration in ticks
    private static final int LED_FADE_TICKS = 12;

    private static final class InterpolationState {
        float renderArmX;
        float renderArmY;
        float renderArmZ;
        long lastUpdateTimeNanos;
        boolean initialized;
    }

    private static final class LedFadeState {
        boolean wasWorking;
        long fadeStartTimeNanos;
        boolean isFading;
    }

    /**
     * Clear interpolation cache for a specific quarry (call when quarry is removed).
     */
    public static void clearInterpolationCache(BlockPos pos) {
        INTERPOLATION_CACHE.remove(pos);
        LED_FADE_CACHE.remove(pos);
    }

    /**
     * Prune cache entries that no longer have a quarry block entity in the current world.
     */
    public static void pruneInterpolationCache(Level world) {
        INTERPOLATION_CACHE.keySet().removeIf(pos -> !(world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity));
        LED_FADE_CACHE.keySet().removeIf(pos -> !(world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity));
    }

    /**
     * Clear all interpolation caches (call on world unload).
     */
    public static void clearAllInterpolationCaches() {
        INTERPOLATION_CACHE.clear();
        LED_FADE_CACHE.clear();
    }

    public LaserQuarryBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            LaserQuarryBlockEntity entity,
            float partialTick,
            PoseStack matrices,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        // Only render arm during mining phase when arm is initialized
        boolean shouldRenderArm = (entity.getCurrentPhase() == QuarryPhase.MINING) && entity.isArmInitialized();
        boolean shouldRenderPreviewOutline = entity.isFreshlyPlaced();

        if (!shouldRenderArm && !shouldRenderPreviewOutline) {
            return;
        }

        Level level = entity.getLevel();
        if (level == null) {
            return;
        }

        BlockPos quarryPos = entity.getBlockPos();
        BlockState blockState = level.getBlockState(quarryPos);

        if (!(blockState.getBlock() instanceof LaserQuarryBlock)) {
            return;
        }

        Direction facing = LaserQuarryBlock.getMiningDirection(blockState);

        // Calculate frame bounds - needed for both arm rendering and preview outline
        int frameStartX;
        int frameStartZ;
        int frameEndX;
        int frameEndZ;

        if (entity.hasCustomBounds()) {
            frameStartX = entity.getCustomMinX();
            frameStartZ = entity.getCustomMinZ();
            frameEndX = entity.getCustomMaxX();
            frameEndZ = entity.getCustomMaxZ();
        } else {
            int half = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) / 2;
            switch (facing) {
                case NORTH:
                    frameStartX = quarryPos.getX() - half;
                    frameStartZ = quarryPos.getZ() - LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);
                    break;
                case SOUTH:
                    frameStartX = quarryPos.getX() - half;
                    frameStartZ = quarryPos.getZ() + 1;
                    break;
                case EAST:
                    frameStartX = quarryPos.getX() + 1;
                    frameStartZ = quarryPos.getZ() - half;
                    break;
                case WEST:
                    frameStartX = quarryPos.getX() - LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);
                    frameStartZ = quarryPos.getZ() - half;
                    break;
                default:
                    return;
            }
            frameEndX = frameStartX + LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 1;
            frameEndZ = frameStartZ + LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 1;
        }
        int frameTopY = quarryPos.getY() + LaserQuarryGeometry.Y_OFFSET_ABOVE;

        if (shouldRenderPreviewOutline) {
            renderFramePreviewOutline(
                    entity, quarryPos, frameStartX, frameStartZ, frameEndX, frameEndZ,
                    matrices, bufferSource, packedOverlay);
        }

        if (!shouldRenderArm) {
            return;
        }

        List<BakedQuad> armModel = MachineModels.quads("laser_quarry_gantry_arm");
        if (armModel.isEmpty()) {
            return;
        }

        // Update client-side interpolation
        float renderArmX = entity.getArmX();
        float renderArmY = entity.getArmY();
        float renderArmZ = entity.getArmZ();

        InterpolationState interp = INTERPOLATION_CACHE.computeIfAbsent(quarryPos, k -> new InterpolationState());
        updateClientInterpolation(interp, renderArmX, renderArmY, renderArmZ, entity.getSyncedArmSpeed());

        renderArmX = interp.renderArmX;
        renderArmY = interp.renderArmY;
        renderArmZ = interp.renderArmZ;

        // Sample light at the center of the frame top for more accurate lighting
        int centerX = (frameStartX + frameEndX) / 2;
        int centerZ = (frameStartZ + frameEndZ) / 2;
        BlockPos frameTopPos = new BlockPos(centerX, frameTopY, centerZ);
        int blockLight = level.getBrightness(LightLayer.BLOCK, frameTopPos);
        int skyLight = level.getBrightness(LightLayer.SKY, frameTopPos);
        int light = LightTexture.pack(blockLight, skyLight);

        // Calculate positions relative to the quarry block (render origin)
        float quarryX = quarryPos.getX();
        float quarryY = quarryPos.getY();
        float quarryZ = quarryPos.getZ();

        float relArmX = renderArmX - quarryX;
        float relArmZ = renderArmZ - quarryZ;
        float relArmY = renderArmY - quarryY;
        float relFrameTopY = frameTopY - quarryY;

        // Calculate beam lengths from actual frame bounds
        int beamLengthX = frameEndX - frameStartX;
        int beamLengthZ = frameEndZ - frameStartZ;

        // East-West beam: at armZ, spanning inside the frame
        renderHorizontalBeam(
                matrices,
                bufferSource,
                armModel,
                light,
                packedOverlay,
                frameStartX + 1 - quarryX,
                relFrameTopY,
                relArmZ,
                beamLengthX,
                true,
                entity);

        // North-South beam: at armX, spanning inside the frame
        renderHorizontalBeam(
                matrices,
                bufferSource,
                armModel,
                light,
                packedOverlay,
                relArmX,
                relFrameTopY,
                frameStartZ + 1 - quarryZ,
                beamLengthZ,
                false,
                entity);

        // Vertical drill beam
        float verticalStartY = relFrameTopY + 0.75f;
        float verticalLength = verticalStartY - relArmY - 1;
        if (verticalLength > 0.1f) {
            renderVerticalBeam(
                    matrices, bufferSource, armModel, light, packedOverlay, relArmX, verticalStartY, relArmZ, verticalLength, entity);
        }

        // Render drill head at the bottom of the vertical beam
        List<BakedQuad> drillModel = MachineModels.quads("laser_quarry_drill");
        if (!drillModel.isEmpty()) {
            matrices.pushPose();
            matrices.translate(relArmX - 0.5, relArmY, relArmZ - 0.5);
            renderModel(entity, drillModel, matrices, bufferSource, light, packedOverlay);
            matrices.popPose();
        }
    }

    /**
     * Update client-side interpolated position to smoothly move towards server position.
     */
    private void updateClientInterpolation(
            InterpolationState interp, float serverArmX, float serverArmY, float serverArmZ, float syncedArmSpeed) {
        long currentTime = System.nanoTime();

        if (!interp.initialized || interp.lastUpdateTimeNanos == 0) {
            // First time - snap to server position
            interp.renderArmX = serverArmX;
            interp.renderArmY = serverArmY;
            interp.renderArmZ = serverArmZ;
            interp.initialized = true;
            interp.lastUpdateTimeNanos = currentTime;
            return;
        }

        // Calculate delta time in seconds
        float deltaSeconds = (currentTime - interp.lastUpdateTimeNanos) / 1_000_000_000f;
        interp.lastUpdateTimeNanos = currentTime;

        // Clamp delta to avoid huge jumps after pauses
        deltaSeconds = Math.min(deltaSeconds, 0.1f);

        // Get current tick rate (MC 1.21.1 always runs at 20 TPS)
        float tickRate = 20f;

        // Speed in blocks per second = synced speed per tick * ticks per second
        float speedPerSecond = syncedArmSpeed * tickRate;
        float moveDistance = speedPerSecond * deltaSeconds;

        // Smoothly interpolate towards server position
        float dx = serverArmX - interp.renderArmX;
        float dy = serverArmY - interp.renderArmY;
        float dz = serverArmZ - interp.renderArmZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= moveDistance) {
            // Close enough, snap to server position
            interp.renderArmX = serverArmX;
            interp.renderArmY = serverArmY;
            interp.renderArmZ = serverArmZ;
        } else {
            // Move towards server position at constant speed
            float factor = moveDistance / distance;
            interp.renderArmX += dx * factor;
            interp.renderArmY += dy * factor;
            interp.renderArmZ += dz * factor;
        }
    }

    /**
     * Render a horizontal beam.
     */
    private void renderHorizontalBeam(
            PoseStack matrices,
            MultiBufferSource bufferSource,
            List<BakedQuad> model,
            int lightmap,
            int overlay,
            float startX,
            float startY,
            float startZ,
            int length,
            boolean alongX,
            LaserQuarryBlockEntity entity) {
        for (int i = 0; i < length; i++) {
            matrices.pushPose();

            if (alongX) {
                matrices.translate(startX + i + 0.5, startY + 0.5, startZ);
                matrices.mulPose(Axis.YP.rotationDegrees(-90));
                matrices.translate(-0.5, -0.5, 0.0);
            } else {
                matrices.translate(startX, startY + 0.5, startZ + i);
                matrices.translate(-0.5, -0.5, -0.5);
            }

            renderModel(entity, model, matrices, bufferSource, lightmap, overlay);
            matrices.popPose();
        }
    }

    /**
     * Render a vertical beam going downward with smooth length.
     */
    private void renderVerticalBeam(
            PoseStack matrices,
            MultiBufferSource bufferSource,
            List<BakedQuad> model,
            int lightmap,
            int overlay,
            float x,
            float startY,
            float z,
            float length,
            LaserQuarryBlockEntity entity) {
        int fullSegments = (int) length;
        float remainder = length - fullSegments;

        // Render partial segment at the TOP first
        if (remainder > 0.1f) {
            matrices.pushPose();
            matrices.translate(x, startY, z);
            matrices.mulPose(Axis.XP.rotationDegrees(90));
            matrices.scale(1.0f, 1.0f, remainder);
            matrices.translate(-0.5, -0.5, 0.0);
            renderModel(entity, model, matrices, bufferSource, lightmap, overlay);
            matrices.popPose();
        }

        // Render full block segments below the partial segment
        for (int i = 0; i < fullSegments; i++) {
            matrices.pushPose();
            matrices.translate(x, startY - remainder - i, z);
            matrices.mulPose(Axis.XP.rotationDegrees(90));
            matrices.translate(-0.5, -0.5, 0.0);
            renderModel(entity, model, matrices, bufferSource, lightmap, overlay);
            matrices.popPose();
        }
    }

    /**
     * Update green LED brightness with fade-out effect.
     */
    private void renderFramePreviewOutline(
            LaserQuarryBlockEntity entity,
            BlockPos quarryPos,
            int frameStartX,
            int frameStartZ,
            int frameEndX,
            int frameEndZ,
            PoseStack matrices,
            MultiBufferSource bufferSource,
            int packedOverlay) {
        List<BakedQuad> beamModel = MachineModels.quads("construction_beam");
        if (beamModel.isEmpty()) {
            return;
        }

        int lightmap = LightTexture.FULL_BRIGHT;

        float qx = quarryPos.getX();
        float qz = quarryPos.getZ();

        float relStartX = frameStartX - qx;
        float relEndX = frameEndX - qx;
        float relStartZ = frameStartZ - qz;
        float relEndZ = frameEndZ - qz;

        int width = frameEndX - frameStartX;
        int depth = frameEndZ - frameStartZ;
        int height = LaserQuarryGeometry.Y_OFFSET_ABOVE;

        // Bottom ring (at quarry Y level, relY = 0)
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, 0, relStartZ, width, 90);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, 0, relEndZ, width, 90);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, 0, relStartZ, depth, 0);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relEndX, 0, relStartZ, depth, 0);

        // Top ring (at quarry Y + Y_OFFSET_ABOVE)
        float topY = LaserQuarryGeometry.Y_OFFSET_ABOVE;
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, topY, relStartZ, width, 90);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, topY, relEndZ, width, 90);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, topY, relStartZ, depth, 0);
        renderOutlineHorizontalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relEndX, topY, relStartZ, depth, 0);

        // Vertical corner edges
        renderOutlineVerticalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, 0, relStartZ, height);
        renderOutlineVerticalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relEndX, 0, relStartZ, height);
        renderOutlineVerticalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relStartX, 0, relEndZ, height);
        renderOutlineVerticalEdge(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay, relEndX, 0, relEndZ, height);
    }

    private void renderOutlineHorizontalEdge(
            LaserQuarryBlockEntity entity,
            List<BakedQuad> beamModel,
            PoseStack matrices,
            MultiBufferSource bufferSource,
            int lightmap,
            int packedOverlay,
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
            renderModel(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay);
            matrices.popPose();
        }
    }

    private void renderOutlineVerticalEdge(
            LaserQuarryBlockEntity entity,
            List<BakedQuad> beamModel,
            PoseStack matrices,
            MultiBufferSource bufferSource,
            int lightmap,
            int packedOverlay,
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
            renderModel(entity, beamModel, matrices, bufferSource, lightmap, packedOverlay);
            matrices.popPose();
        }
    }

    /**
     * Renders code-generated quads at the current transformation using cutout render type.
     */
    private void renderModel(
            LaserQuarryBlockEntity entity,
            List<BakedQuad> model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        CodeModelRenderer.draw(model, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Renders code-generated quads at the current transformation using translucent render type.
     */
    private void renderModelTranslucent(
            LaserQuarryBlockEntity entity,
            List<BakedQuad> model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        CodeModelRenderer.draw(model, poseStack, buffer, packedLight, packedOverlay);
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
    public boolean shouldRenderOffScreen(LaserQuarryBlockEntity blockEntity) {
        return true;
    }
}
