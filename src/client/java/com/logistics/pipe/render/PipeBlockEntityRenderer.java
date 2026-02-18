package com.logistics.pipe.render;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.TravelingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Renders pipe block entities: dynamic core/arm geometry and traveling items.
 *
 * <p>Pipe rendering is fully dynamic (no baked blockstate) because connection
 * directions and module state change at runtime. The renderer:
 * <ol>
 *   <li>Renders the pipe core model (module-overridable per pipe type)
 *   <li>Renders core decoration overlays with per-decoration tint colors
 *   <li>Renders a directional arm for each connected face (module-overridable)
 *   <li>Renders traveling items at interpolated positions along the pipe
 * </ol>
 *
 * <p>All arm models are NORTH-facing in their JSON definitions and are rotated
 * at render time to face the correct direction.
 *
 * <p>Tinting is applied per-quad: quads with tintindex receive the model's
 * designated color; untinted quads render at full white.
 */
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {
    private static final float BLOCK_OFFSET = 0.3125f;
    private static final float ITEM_OFFSET = 0.375f;

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(
            PipeBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return;

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null || entity.getLevel() == null) return;

        PipeContext ctx = new PipeContext(entity.getLevel(), entity.getBlockPos(), state, entity);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        // Render core model
        renderModel(getModel(pipe.getCoreModelId(ctx)), null, 0xFFFFFF, state, poseStack, buffer, packedLight, packedOverlay);

        // Render core decorations (e.g., pipe markings color overlay)
        for (Pipe.CoreDecoration decoration : pipe.getCoreDecorations(ctx)) {
            renderModel(getModel(decoration.modelId()), null, decoration.color(), state, poseStack, buffer, packedLight, packedOverlay);
        }

        // Render an arm for each connected direction
        for (Direction direction : Direction.values()) {
            if (entity.getCachedConnectionType(direction) == PipeConnection.Type.NONE) continue;

            ResourceLocation armId = pipe.getPipeArm(ctx, direction);
            Integer armTint = pipe.getArmTint(ctx, direction);
            int armColor = armTint != null ? armTint : 0xFFFFFF;

            renderModel(getModel(armId), direction, armColor, state, poseStack, buffer, packedLight, packedOverlay);

            for (ResourceLocation decoration : pipe.getPipeDecorations(ctx, direction)) {
                renderModel(getModel(decoration), direction, 0xFFFFFF, state, poseStack, buffer, packedLight, packedOverlay);
            }
        }

        // Get pipe speed properties for physics-based item interpolation
        float maxSpeed = pipe.getMaxSpeed(ctx);
        float accelerationRate = pipe.getAccelerationRate(ctx);
        float dragCoefficient = pipe.getDrag(ctx);

        // Render items traveling through the pipe
        List<TravelingItem> travelingItems = entity.getTravelingItems();
        for (TravelingItem item : travelingItems) {
            renderTravelingItem(item, partialTick, maxSpeed, accelerationRate, dragCoefficient,
                    poseStack, bufferSource, packedLight, packedOverlay, entity);
        }
    }

    private void renderModel(
            BakedModel model,
            Direction armDirection,
            int tintColor,
            BlockState state,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay) {

        if (model == null) return;

        if (armDirection != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            applyDirectionRotation(poseStack, armDirection);
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        renderModelQuads(model, tintColor, state, poseStack, buffer, packedLight, packedOverlay);

        if (armDirection != null) {
            poseStack.popPose();
        }
    }

    private void renderModelQuads(
            BakedModel model,
            int tintColor,
            BlockState state,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay) {

        float r = (tintColor >> 16 & 0xFF) / 255.0f;
        float g = (tintColor >> 8 & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;

        RandomSource random = RandomSource.create(42L);

        // General quads (not face-culled)
        renderQuads(model.getQuads(state, null, random), r, g, b, poseStack, buffer, packedLight, packedOverlay);

        // Face-specific quads (can be culled but we render all since pipes use RenderShape.INVISIBLE)
        for (Direction face : Direction.values()) {
            random.setSeed(42L);
            renderQuads(model.getQuads(state, face, random), r, g, b, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    /**
     * Renders a list of quads, applying the tint color only to quads that declare a tintindex.
     * Untinted quads render at full white (1, 1, 1).
     */
    private void renderQuads(
            List<BakedQuad> quads,
            float tintR,
            float tintG,
            float tintB,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay) {

        for (BakedQuad quad : quads) {
            float r = quad.isTinted() ? tintR : 1.0f;
            float g = quad.isTinted() ? tintG : 1.0f;
            float b = quad.isTinted() ? tintB : 1.0f;
            buffer.putBulkData(poseStack.last(), quad, r, g, b, 1.0f, packedLight, packedOverlay);
        }
    }

    /**
     * Renders a traveling item at its interpolated position within the pipe.
     * Items move from the entry face (progress=0) to the exit face (progress=1).
     */
    private void renderTravelingItem(
            TravelingItem item,
            float partialTick,
            float maxSpeed,
            float accelerationRate,
            float dragCoefficient,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            PipeBlockEntity entity) {

        poseStack.pushPose();

        // Ground display context renders items slightly above the "ground", offset Y to center in pipe
        float yOffset = item.getStack().getItem() instanceof BlockItem ? BLOCK_OFFSET : ITEM_OFFSET;
        poseStack.translate(0.5, yOffset, 0.5);

        // Calculate speed change during this partial tick
        float speedChange = 0f;
        boolean deceleratingToMax = item.getSpeed() > maxSpeed;
        if (deceleratingToMax) {
            float remaining = Math.max(1.0e-4f, 1.0f - item.getProgress());
            float targetSquared = maxSpeed * maxSpeed;
            float currentSquared = item.getSpeed() * item.getSpeed();
            float decel = (targetSquared - currentSquared) / (2.0f * remaining);
            speedChange = decel * partialTick;
        } else if (accelerationRate != 0f) {
            speedChange = accelerationRate * partialTick;
        } else if (dragCoefficient != 0f) {
            speedChange = -(item.getSpeed() * dragCoefficient) * partialTick;
        }

        // Speed at the end of this partial tick
        float interpolatedSpeed = item.getSpeed() + speedChange;
        if (interpolatedSpeed < LogisticsPipe.CONFIG.ITEM_MIN_SPEED) {
            interpolatedSpeed = LogisticsPipe.CONFIG.ITEM_MIN_SPEED;
        } else if (!deceleratingToMax && interpolatedSpeed > maxSpeed) {
            interpolatedSpeed = maxSpeed;
        }

        // Use average speed for progress calculation (trapezoidal integration)
        float avgSpeed = (item.getSpeed() + interpolatedSpeed) / 2.0f;
        float interpolatedProgress = item.getProgress() + (avgSpeed * partialTick);
        float travelOffset = interpolatedProgress - 0.5f;

        Direction dir = item.getDirection();
        poseStack.translate(
                dir.getStepX() * travelOffset,
                dir.getStepY() * travelOffset,
                dir.getStepZ() * travelOffset);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item.getStack(),
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                entity.getLevel(),
                0);

        poseStack.popPose();
    }

    private BakedModel getModel(ResourceLocation id) {
        FabricBakedModelManager modelManager = (FabricBakedModelManager) Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(id);

        if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return null;
        }

        return model;
    }

    /**
     * Rotates the pose stack so a NORTH-facing arm model points in the given direction.
     * Arm JSON models are authored to exit toward NORTH (−Z face, z=[0,4]).
     */
    private static void applyDirectionRotation(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            default    -> {} // NORTH: base model orientation, no rotation needed
        }
    }
}
