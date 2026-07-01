package com.logistics.automation.crucible;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the tank fluid through the window in the crucible's front face: the fluid's still sprite
 * (via {@link FluidBoxRenderer}, UV-mapped by block position so it tiles rather than stretches), filling
 * from the window bottom (pixel 11) up to the top (pixel 4) as the tank fills. Rotated to the block facing.
 */
public class MagmaCrucibleBlockEntityRenderer
        implements BlockEntityRenderer<MagmaCrucibleBlockEntity, MagmaCrucibleRenderState> {

    // Window in the face texture: x 6..10, y 4..12 px (block-local; y measured from the block bottom).
    private static final float X0 = 6f / 16f;
    private static final float X1 = 10f / 16f;
    private static final float Y_BOTTOM = 4f / 16f;
    private static final float Y_TOP = 12f / 16f;
    // A thin slab just inside the front face so the fluid reads as sitting behind the glass.
    private static final float Z_FRONT = 0.02f;
    private static final float Z_BACK = 0.10f;

    public MagmaCrucibleBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public MagmaCrucibleRenderState createRenderState() {
        return new MagmaCrucibleRenderState();
    }

    @Override
    public void extractRenderState(
            MagmaCrucibleBlockEntity entity,
            MagmaCrucibleRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        var tank = entity.tank();
        long amount = tank.getAmount();
        long capacity = tank.getCapacity();
        Level level = entity.getLevel();
        state.facing = entity.getBlockState().getValue(MagmaCrucibleBlock.FACING);
        state.hasFluid = amount > 0 && capacity > 0 && !tank.isEmpty() && level != null;
        state.fillRatio = state.hasFluid ? Math.min(1.0F, (float) amount / capacity) : 0.0F;
        if (!state.hasFluid) {
            state.sprite = null;
            return;
        }

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
            MagmaCrucibleRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        if (!state.hasFluid || state.sprite == null || state.fillRatio <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = state.sprite;
        int color = FluidBoxRenderer.opaque(state.tintColor);
        int light = state.lightCoords;
        float top = Y_BOTTOM + Math.min(1.0F, state.fillRatio) * (Y_TOP - Y_BOTTOM);
        // Rotate the north-default window onto the block's facing (matches the blockstate y-rotation).
        float angle = (state.facing.toYRot() + 180.0F) % 360.0F;

        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(-angle));
        matrices.translate(-0.5, -0.5, -0.5);
        queue.submitCustomGeometry(
                matrices,
                RenderTypes.translucentMovingBlock(),
                (entry, buffer) -> FluidBoxRenderer.renderBox(
                        entry, buffer, sprite, color, light, X0, Y_BOTTOM, Z_FRONT, X1, top, Z_BACK, true, false));
        matrices.popPose();
    }
}
