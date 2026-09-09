package com.logistics.pipe.render;

import com.logistics.core.lib.client.render.MachineModels;
import com.logistics.automation.pump.FluidPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class FluidPumpBlockEntityRenderer implements BlockEntityRenderer<FluidPumpBlockEntity, FluidPumpRenderState> {
    public FluidPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public FluidPumpRenderState createRenderState() {
        return new FluidPumpRenderState();
    }

    @Override
    public void extractRenderState(
            FluidPumpBlockEntity entity,
            FluidPumpRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.pumpPos = entity.getBlockPos();
        state.armY = entity.armY();
        state.tubeLight = state.lightCoords;
        Level level = entity.getLevel();
        if (level != null) {
            BlockPos lightPos = BlockPos.containing(
                    state.pumpPos.getX() + 0.5, Math.min(state.pumpPos.getY(), state.armY), state.pumpPos.getZ() + 0.5);
            int blockLight = level.getBrightness(LightLayer.BLOCK, lightPos);
            int skyLight = level.getBrightness(LightLayer.SKY, lightPos);
            state.tubeLight = LightCoordsUtil.pack(blockLight, skyLight);
        }
    }

    @Override
    public void submit(
            FluidPumpRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        // Connect at the pump's bottom face so the top tile's overshoot hides inside the pump.
        float tubeStartY = 0.0f;
        float length = state.pumpPos.getY() + tubeStartY - state.armY;
        if (length <= 0.05f) {
            return;
        }

        List<BlockStateModelPart> parts = MachineModels.parts("fluid_pump_tube");
        if (parts.isEmpty()) {
            return;
        }

        RenderType renderLayer = RenderTypes.cutoutMovingBlock();

        // Render only whole, uniform tiles anchored to the arm tip so the texture never stretches.
        // The top tile overshoots up into the opaque pump block, hiding the discrete tile-count
        // change as the pipe grows -> smooth, non-choppy descent at any depth.
        int tiles = (int) Math.ceil(length);
        float tip = tubeStartY - length;
        for (int i = 0; i < tiles; i++) {
            renderSegment(matrices, queue, parts, renderLayer, state.tubeLight, tip + i, 1.0f);
        }
    }

    private void renderSegment(
            PoseStack matrices,
            SubmitNodeCollector queue,
            List<BlockStateModelPart> parts,
            RenderType renderLayer,
            int light,
            float yBottom,
            float length) {
        matrices.pushPose();
        matrices.translate(0.0f, yBottom, 0.0f);
        matrices.scale(1.0f, length, 1.0f);
        queue.submitBlockModel(matrices, renderLayer, parts, new int[]{0xFFFFFF}, light, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }

    /**
     * The intake tube follows the fluid surface down, so it routinely reaches far past the 64-block
     * default and past its own block's section. Both gates are checked independently: this one is
     * the distance radius, {@link #shouldRenderOffScreen()} is the frustum escape.
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
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
