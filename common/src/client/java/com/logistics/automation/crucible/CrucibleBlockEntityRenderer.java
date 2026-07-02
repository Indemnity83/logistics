package com.logistics.automation.crucible;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Renders the tank fluid as a gauge on the crucible's front face: the fluid's still sprite (via
 * {@link FluidBoxRenderer}, UV-mapped by block position so it tiles rather than stretches), drawn on top
 * of the (solid) face and clipped to the window region — filling from the window bottom (pixel 4) up to
 * the top (pixel 12) as the tank fills. Rotated to the block facing.
 *
 * <p>MC 1.21.1 uses the classic {@link BlockEntityRenderer} API (no render-state / SubmitNodeCollector):
 * the face gauge is drawn directly into a {@code translucent} buffer.
 */
public class CrucibleBlockEntityRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {

    // Window in the face texture: x 6..10, y 4..12 px (block-local; y measured from the block bottom).
    private static final float X0 = 6f / 16f;
    private static final float X1 = 10f / 16f;
    private static final float Y_BOTTOM = 4f / 16f;
    private static final float Y_TOP = 12f / 16f;
    // A flat plane a hair proud of the front face (north's outside is -Z), so the fluid sits flush on the
    // solid face — visible whether lit or not, with no depth gap and no see-through above the fill line.
    private static final float Z_FACE = -0.002f;

    public CrucibleBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            CrucibleBlockEntity entity,
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

        Direction facing = entity.getBlockState().getValue(CrucibleBlock.FACING);
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
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        FluidBoxRenderer.renderFaceQuad(poseStack.last(), buffer, sprite, color, light, X0, Y_BOTTOM, X1, top, Z_FACE);
        poseStack.popPose();
    }
}
