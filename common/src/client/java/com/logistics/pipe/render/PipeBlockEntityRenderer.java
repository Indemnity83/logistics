package com.logistics.pipe.render;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.DebugLog;
import com.logistics.core.lib.client.render.VanillaQuadBaker;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ItemPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.render.model.PipeGeometry;
import com.logistics.pipe.render.model.PipeModelResolver;
import com.logistics.core.lib.pipe.TravelingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String PIPE_MODEL_PREFIX = "block/pipe/";

    private final RenderProfiler profiler = new RenderProfiler();

    // Generated geometry never changes, so cache built quads per model id and resolved sprites.
    private final Map<ResourceId, List<BakedQuad>> codeQuadCache = new HashMap<>();
    private final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    /** Build (and cache) the code-generated quads for a pipe part model id. */
    private List<BakedQuad> codeQuads(ResourceId modelId) {
        return codeQuadCache.computeIfAbsent(modelId, id -> {
            String base = id.getPath().substring(PIPE_MODEL_PREFIX.length());
            List<BakedQuad> out = new ArrayList<>();
            for (PipeModelResolver.Layer layer : PipeModelResolver.resolve(base)) {
                TextureAtlasSprite sprite = sprite(layer.textureBase());
                VanillaQuadBaker baker = new VanillaQuadBaker(sprite, layer.tintIndex(), true, 0);
                PipeGeometry.emit(baker::quad, layer.shape(), sprite);
                out.addAll(baker.toQuads());
            }
            return out;
        });
    }

    private TextureAtlasSprite sprite(String textureBase) {
        return spriteCache.computeIfAbsent(textureBase, b -> Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceId.in("logistics", "block/pipe/" + b).toIdentifier()));
    }

    /** Draw code-generated quads, reusing the same rotation + putBulkData path as JSON models. */
    private void renderCodeModel(ResourceId modelId, Direction armDirection, int tintColor,
            PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        List<BakedQuad> quads = codeQuads(modelId);
        if (quads.isEmpty()) return;

        if (armDirection != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            applyDirectionRotation(poseStack, armDirection);
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        float r = (tintColor >> 16 & 0xFF) / 255.0f;
        float g = (tintColor >> 8 & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        renderQuads(quads, r, g, b, poseStack, buffer, packedLight, packedOverlay);

        if (armDirection != null) {
            poseStack.popPose();
        }
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

        ItemPipe pipe = pipeBlock.getPipe();
        if (pipe == null || entity.getLevel() == null) return;

        boolean debugRender = DebugLog.isEnabled("render");
        long t0 = debugRender ? profiler.startSubmit() : 0;

        PipeContext ctx = new PipeContext(entity.getLevel(), entity.getBlockPos(), state, entity);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        // Render core model (geometry generated in code). Logistics-pipe cores carry a tintable
        // power-status overlay (green powered / red unpowered); other cores return null -> untinted.
        Integer coreTint = pipe.getCoreTint(ctx);
        int coreColor = coreTint != null ? coreTint : 0xFFFFFF;
        renderCodeModel(pipe.getCoreModelId(ctx), null, coreColor, poseStack, buffer, packedLight, packedOverlay);

        // Render core decorations (e.g., pipe markings color overlay)
        for (CoreDecoration decoration : pipe.getCoreDecorations(ctx)) {
            renderCodeModel(decoration.modelId(), null, decoration.color(), poseStack, buffer, packedLight, packedOverlay);
        }

        // Render an arm for each connected direction
        for (Direction direction : Direction.values()) {
            if (entity.getCachedConnectionType(direction) == PipeConnection.Type.NONE) continue;

            Integer armTint = pipe.getArmTint(ctx, direction);
            int armColor = armTint != null ? armTint : 0xFFFFFF;
            renderCodeModel(pipe.getPipeArm(ctx, direction), direction, armColor, poseStack, buffer, packedLight, packedOverlay);

            for (ResourceId decoration : pipe.getPipeDecorations(ctx, direction)) {
                renderCodeModel(decoration, direction, 0xFFFFFF, poseStack, buffer, packedLight, packedOverlay);
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

        if (debugRender) profiler.endSubmit(t0);
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
        if (interpolatedSpeed < LogisticsConfig.get().pipe.minSpeed) {
            interpolatedSpeed = LogisticsConfig.get().pipe.minSpeed;
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
