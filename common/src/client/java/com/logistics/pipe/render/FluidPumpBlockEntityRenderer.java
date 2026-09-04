package com.logistics.pipe.render;

import com.logistics.core.lib.client.render.MachineModels;
import com.logistics.automation.pump.FluidPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class FluidPumpBlockEntityRenderer implements BlockEntityRenderer<FluidPumpBlockEntity> {
    public FluidPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            FluidPumpBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pumpPos = entity.getBlockPos();
        float armY = entity.armY();
        // Connect at the pump's bottom face so the top tile's overshoot hides inside the pump.
        float length = pumpPos.getY() - armY;
        if (length <= 0.05f) {
            return;
        }

        List<BakedQuad> quads = MachineModels.quads("fluid_pump_tube");
        if (quads.isEmpty()) {
            return;
        }

        // Light the tube from the arm tip (often deep underground), not the pump block above it.
        BlockPos lightPos = BlockPos.containing(
                pumpPos.getX() + 0.5, Math.min(pumpPos.getY(), armY), pumpPos.getZ() + 0.5);
        int light = LightTexture.pack(
                level.getBrightness(LightLayer.BLOCK, lightPos), level.getBrightness(LightLayer.SKY, lightPos));

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        // Render only whole, uniform tiles anchored to the arm tip so the texture never stretches.
        // The top tile overshoots up into the opaque pump block, hiding the discrete tile-count
        // change as the pipe grows -> smooth, non-choppy descent at any depth.
        int tiles = (int) Math.ceil(length);
        float tip = -length;
        for (int i = 0; i < tiles; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0f, tip + i, 0.0f);
            for (BakedQuad quad : quads) {
                buffer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, light, packedOverlay);
            }
            poseStack.popPose();
        }
    }

    /**
     * The intake tube follows the fluid surface down, so it routinely reaches far past the 64-block
     * default and past its own block's section. Both gates are checked independently: this one is
     * the distance radius, {@link #shouldRenderOffScreen} is the frustum escape.
     */
    @Override
    public int getViewDistance() {
        return 256;
    }

    /**
     * Renders even when the pump's own section is out of frustum — standing beside a long tube
     * looking along it would otherwise make the whole tube disappear. Same reason vanilla's beacon
     * beam does this.
     */
    @Override
    public boolean shouldRenderOffScreen(FluidPumpBlockEntity blockEntity) {
        return true;
    }
}
