package com.logistics.automation.refinery;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the output tank's fluid as a gauge on the refinery's front face, mirroring the crucible: the
 * fluid's still sprite is drawn on top of the (solid) face, clipped to the window and filling from the
 * window bottom up as the tank fills, rotated to the block facing.
 *
 * <p>Window in the face texture: x 2..7, y 6..13 px (top-down). MC's north-face UV mirrors x and y
 * ({@code u=16-x}, {@code v=16-y}), so the block-local window is x 9..14, y 3..10 (measured from the
 * block's west/bottom).
 */
public class RefineryBlockEntityRenderer
        implements BlockEntityRenderer<RefineryBlockEntity, RefineryRenderState> {

    private static final float X0 = 9f / 16f;
    private static final float X1 = 14f / 16f;
    private static final float Y_BOTTOM = 3f / 16f;
    private static final float Y_TOP = 10f / 16f;
    // A flat plane a hair proud of the front face (north's outside is -Z), so the fluid sits flush on the
    // solid face — visible whether lit or not, with no depth gap and no see-through above the fill line.
    private static final float Z_FACE = -0.002f;

    public RefineryBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public RefineryRenderState createRenderState() {
        return new RefineryRenderState();
    }

    @Override
    public void extractRenderState(
            RefineryBlockEntity entity,
            RefineryRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        var tank = entity.tank();
        long amount = tank.getAmount();
        long capacity = tank.getCapacity();
        Level level = entity.getLevel();
        state.facing = entity.getBlockState().getValue(RefineryBlock.FACING);
        state.hasFluid = amount > 0 && capacity > 0 && !tank.isEmpty() && level != null;
        state.fillRatio = state.hasFluid ? Math.min(1.0F, (float) amount / capacity) : 0.0F;
        if (!state.hasFluid) {
            state.sprite = null;
            return;
        }

        // Light the gauge from the exposed neighbour in front of the face (packed sky<<20 | block<<4).
        BlockPos facePos = entity.getBlockPos().relative(state.facing);
        int sky = level.getBrightness(LightLayer.SKY, facePos);
        int block = level.getBrightness(LightLayer.BLOCK, facePos);
        state.faceLight = (sky << 20) | (block << 4);

        FluidBoxRenderer.Appearance appearance =
                FluidBoxRenderer.resolve(tank.getFluidKey().getFluid(), level, entity.getBlockPos());
        if (appearance == null) {
            state.hasFluid = false;
            state.sprite = null;
            return;
        }
        state.sprite = appearance.sprite();
        state.tintColor = appearance.tint();
    }

    @Override
    public void submit(
            RefineryRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        if (!state.hasFluid || state.sprite == null || state.fillRatio <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = state.sprite;
        int color = FluidBoxRenderer.opaque(state.tintColor);
        int light = state.faceLight;
        float top = Y_BOTTOM + Math.min(1.0F, state.fillRatio) * (Y_TOP - Y_BOTTOM);
        // Rotate the north-default window onto the block's facing (matches the blockstate y-rotation).
        float angle = (state.facing.toYRot() + 180.0F) % 360.0F;

        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.rotate(Axis.YP.rotationDegrees(-angle));
        matrices.translate(-0.5, -0.5, -0.5);
        queue.submitCustomGeometry(
                matrices,
                RenderTypes.translucentMovingBlock(),
                (entry, buffer) ->
                        FluidBoxRenderer.renderFaceQuad(entry, buffer, sprite, color, light, X0, Y_BOTTOM, X1, top, Z_FACE));
        matrices.popPose();
    }
}
