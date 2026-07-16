package com.logistics.automation.refinery;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Renders the output tank's fluid as a gauge on the refinery's front face, mirroring the crucible: the
 * fluid's still sprite is drawn on top of the (solid) face, clipped to the window and filling from the
 * window bottom up as the tank fills, rotated to the block facing.
 *
 * <p>Window in the face texture: x 2..7, y 6..13 px (top-down). MC's north-face UV mirrors x and y
 * ({@code u=16-x}, {@code v=16-y}), so the block-local window is x 9..14, y 3..10 (measured from the
 * block's west/bottom).
 *
 * <p>MC 1.21.1 uses the classic {@link BlockEntityRenderer} API (no render-state / SubmitNodeCollector).
 */
public class RefineryBlockEntityRenderer implements BlockEntityRenderer<RefineryBlockEntity> {

    private static final float X0 = 9f / 16f;
    private static final float X1 = 14f / 16f;
    private static final float Y_BOTTOM = 3f / 16f;
    private static final float Y_TOP = 10f / 16f;
    // A flat plane a hair proud of the front face (north's outside is -Z), so the fluid sits flush on the
    // solid face — visible whether lit or not, with no depth gap and no see-through above the fill line.
    private static final float Z_FACE = -0.002f;

    public RefineryBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            RefineryBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        var tank = entity.tank();
        long amount = tank.getAmount();
        long capacity = tank.getCapacity();
        Level level = entity.getLevel();
        boolean hasFluid = amount > 0 && capacity > 0 && !tank.isEmpty() && level != null;
        if (!hasFluid) {
            return;
        }
        float fillRatio = Math.min(1.0F, (float) amount / capacity);
        if (fillRatio <= 0.0F) {
            return;
        }

        FluidBoxRenderer.Appearance appearance =
                FluidBoxRenderer.resolve(tank.getFluidKey().getFluid(), level, entity.getBlockPos());
        if (appearance == null) {
            return;
        }
        TextureAtlasSprite sprite = appearance.sprite();
        int color = FluidBoxRenderer.opaque(appearance.tint());

        Direction facing = entity.getBlockState().getValue(RefineryBlock.FACING);
        // Light the gauge from the exposed neighbour in front of the face (packed sky<<20 | block<<4).
        BlockPos facePos = entity.getBlockPos().relative(facing);
        int sky = level.getBrightness(LightLayer.SKY, facePos);
        int block = level.getBrightness(LightLayer.BLOCK, facePos);
        int light = (sky << 20) | (block << 4);

        float top = Y_BOTTOM + fillRatio * (Y_TOP - Y_BOTTOM);
        // Rotate the north-default window onto the block's facing (matches the blockstate y-rotation).
        float angle = (facing.toYRot() + 180.0F) % 360.0F;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        poseStack.translate(-0.5, -0.5, -0.5);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        FluidBoxRenderer.renderFaceQuad(poseStack.last(), buffer, sprite, color, light, X0, Y_BOTTOM, X1, top, Z_FACE);
        poseStack.popPose();
    }
}
