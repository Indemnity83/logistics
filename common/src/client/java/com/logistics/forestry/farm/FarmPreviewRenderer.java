package com.logistics.forestry.farm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Draws a farm's area preview ({@link FarmPreviewLines}) with vanilla lines, unlit, while the farm
 * surveys or for a while after Show area or an upgrade. The lines reach well past the farm's own
 * section, so it is drawn off screen and from further away than a block entity usually is.
 *
 * <p>MC 1.21.1 has no render-state extraction: the lines are read and drawn in one {@code render}
 * pass, and the line width comes from {@link RenderType#lines()}'s own state.
 */
public class FarmPreviewRenderer implements BlockEntityRenderer<FarmBlockEntity> {

    private final FarmPreviewLineCache<FarmBlockEntity> cache = new FarmPreviewLineCache<>(FarmPreviewRenderer::build);

    public FarmPreviewRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            FarmBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        Level level = entity.getLevel();
        FarmPreview preview = entity.preview();
        if (level == null || !preview.visible(level.getGameTime())) {
            cache.forget(entity);
            return;
        }
        List<FarmPreviewLines.Line> lines = cache.lines(entity, preview, entity.getBlockPos());
        if (lines.isEmpty()) {
            return;
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        for (FarmPreviewLines.Line line : lines) {
            emit(pose, buffer, line);
        }
    }

    private static List<FarmPreviewLines.Line> build(FarmPreview preview, BlockPos origin) {
        return FarmPreviewLines.of(
                preview.inner(),
                preview.outer(),
                preview.height(),
                relative(preview.survey().flaggedAt(), origin),
                relative(preview.survey().protectedAt(), origin));
    }

    private static List<BlockPos> relative(List<BlockPos> positions, BlockPos origin) {
        List<BlockPos> relative = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            relative.add(pos.subtract(origin));
        }
        return relative;
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, FarmPreviewLines.Line line) {
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
                .setNormal(pose, dx, dy, dz);
        buffer.addVertex(pose, line.x1(), line.y1(), line.z1())
                .setColor(line.color())
                .setNormal(pose, dx, dy, dz);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(FarmBlockEntity entity) {
        return true;
    }
}
