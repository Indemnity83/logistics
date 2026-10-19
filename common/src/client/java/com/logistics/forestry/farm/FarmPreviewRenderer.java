package com.logistics.forestry.farm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a farm's area preview ({@link FarmPreviewLines}) with vanilla lines, unlit, while the farm
 * surveys or for a while after Show area or an upgrade. The lines reach well past the farm's own
 * section, so it is drawn off screen and from further away than a block entity usually is.
 */
public class FarmPreviewRenderer implements BlockEntityRenderer<FarmBlockEntity, FarmPreviewRenderState> {

    public FarmPreviewRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public FarmPreviewRenderState createRenderState() {
        return new FarmPreviewRenderState();
    }

    @Override
    public void extractRenderState(
            FarmBlockEntity entity,
            FarmPreviewRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        Level level = entity.getLevel();
        FarmPreview preview = entity.preview();
        state.visible = level != null && preview.visible(level.getGameTime());
        if (!state.visible) {
            state.lines = List.of();
            return;
        }
        BlockPos origin = entity.getBlockPos();
        state.lines = FarmPreviewLines.of(
                preview.inner(),
                preview.outer(),
                preview.height(),
                relative(preview.survey().flaggedAt(), origin),
                relative(preview.survey().protectedAt(), origin));
        state.lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
    }

    private static List<BlockPos> relative(List<BlockPos> positions, BlockPos origin) {
        List<BlockPos> relative = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            relative.add(pos.subtract(origin));
        }
        return relative;
    }

    @Override
    public void submit(
            FarmPreviewRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.visible || state.lines.isEmpty()) {
            return;
        }
        List<FarmPreviewLines.Line> lines = state.lines;
        float width = state.lineWidth;
        queue.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, buffer) -> {
            for (FarmPreviewLines.Line line : lines) {
                emit(pose, buffer, line, width);
            }
        });
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, FarmPreviewLines.Line line, float width) {
        float dx = line.x1() - line.x0();
        float dy = line.y1() - line.y0();
        float dz = line.z1() - line.z0();
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        buffer.addVertex(pose, line.x0(), line.y0(), line.z0())
                .setColor(line.color())
                .setNormal(pose, dx, dy, dz)
                .setLineWidth(width);
        buffer.addVertex(pose, line.x1(), line.y1(), line.z1())
                .setColor(line.color())
                .setNormal(pose, dx, dy, dz)
                .setLineWidth(width);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
