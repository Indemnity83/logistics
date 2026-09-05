package com.logistics.pipe.render;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.DebugLog;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.client.render.VanillaQuadBaker;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.LogisticsMod;
import com.logistics.pipe.Pipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.render.model.PipeGeometry;
import com.logistics.pipe.render.model.PipeModelResolver;
import com.logistics.core.lib.pipe.TravelingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renders traveling items inside pipes
 */
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeRenderState> {
    private static final float BLOCK_OFFSET = 0.3125f;
    private static final float ITEM_OFFSET = 0.375f;
    private static final String PIPE_MODEL_PREFIX = "block/pipe/";

    private final ItemModelResolver itemModelManager;

    // Generated geometry never changes at runtime — cache built models by id, sprites by texture base.
    private final Map<ResourceId, BlockStateModel> modelsCache = new HashMap<>();
    private final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

    // Per-renderer profiler instance — one profiler per renderer (one renderer per pipe type)
    private final RenderProfiler profiler = new RenderProfiler();

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelManager = ctx.itemModelResolver();
    }

    /**
     * Build a code-generated model for a pipe model id (core/arm/decoration). Each resolved layer
     * becomes one procedurally baked part; on 1.21.11 {@code submitBlockModel} takes a model, so the
     * parts are wrapped in a {@link BlockStateModel}.
     */
    private BlockStateModel buildModel(ResourceId modelId) {
        // Pipe model ids are always produced as logistics:block/pipe/<name> (see Pipe#getCoreModelId /
        // #getPipeArm). Enforce that contract and fail fast on a stray id, rather than silently
        // collapsing to a basename + hardcoded prefix and resolving the wrong sprite.
        if (!"logistics".equals(modelId.getNamespace()) || !modelId.getPath().startsWith(PIPE_MODEL_PREFIX)) {
            throw new IllegalStateException("Unexpected pipe model id (expected logistics:block/pipe/...): " + modelId);
        }
        String base = modelId.getPath().substring(PIPE_MODEL_PREFIX.length());

        List<BlockModelPart> parts = new ArrayList<>();
        TextureAtlasSprite particle = null;
        for (PipeModelResolver.Layer layer : PipeModelResolver.resolve(base)) {
            TextureAtlasSprite sprite = sprite(layer.textureBase());
            if (particle == null) {
                particle = sprite;
            }
            VanillaQuadBaker baker = new VanillaQuadBaker(sprite, layer.tintIndex(), true, 0);
            PipeGeometry.emit(baker::quad, layer.shape(), sprite);
            if (!baker.isEmpty()) {
                parts.add(baker.toPart());
            }
        }
        return new ProceduralModel(List.copyOf(parts), particle);
    }

    private TextureAtlasSprite sprite(String textureBase) {
        return spriteCache.computeIfAbsent(textureBase, b -> Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(LogisticsMod.modId("block/pipe/" + b).toIdentifier()));
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void extractRenderState(
            PipeBlockEntity entity,
            PipeRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {

        boolean debugRender = DebugLog.isEnabled("render");
        long t0 = debugRender ? profiler.startExtract() : 0;

        // Update base block entity render state
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        // Store tickDelta for use in render()
        state.tickDelta = tickDelta;

        BlockState blockState = entity.getBlockState();
        state.blockState = blockState;

        state.models.clear();
        {

            // Get pipe properties for speed calculations
            float maxSpeed = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MAX_SPEED);
            float accelerationRate = 0f;
            float dragCoefficient = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_DRAG);

            if (blockState.getBlock() instanceof PipeBlock pipeBlock) {
                if (pipeBlock.getPipe() != null && entity.getLevel() != null) {
                    PipeContext context = new PipeContext(entity.getLevel(), entity.getBlockPos(), blockState, entity);
                    maxSpeed = pipeBlock.getPipe().getMaxSpeed(context);
                    accelerationRate = pipeBlock.getPipe().getAccelerationRate(context);
                    dragCoefficient = pipeBlock.getPipe().getDrag(context);

                    Pipe pipe = pipeBlock.getPipe();
                    // Logistics-pipe cores carry a tintable power-status overlay; the module decides
                    // the color (green when this pipe is linked into a powered network, red
                    // otherwise). Cores without that overlay return null here and render untinted.
                    Integer coreTint = pipe.getCoreTint(context);
                    int coreColor = coreTint != null ? coreTint : 0xFFFFFF;
                    state.models.add(buildModelInfo(pipe.getCoreModelId(context), coreColor));
                    for (CoreDecoration decoration : pipe.getCoreDecorations(context)) {
                        state.models.add(buildModelInfo(decoration.modelId(), decoration.color()));
                    }

                    for (Direction direction : Direction.values()) {
                        PipeConnection.Type type = entity.getCachedConnectionType(direction);
                        if (type == PipeConnection.Type.NONE) {
                            continue;
                        }

                        // Get arm model (module override or base), rotate at render time
                        var armModel = pipe.getPipeArm(context, direction);
                        Integer armTint = pipe.getArmTint(context, direction);
                        int armColor = armTint != null ? armTint : 0xFFFFFF;
                        state.models.add(buildModelInfo(armModel, armColor, direction));

                        for (var decoration : pipe.getPipeDecorations(context, direction)) {
                            // Decorations are scoped to the arm they belong to, so they rotate with it.
                            state.models.add(buildModelInfo(decoration, 0xFFFFFF, direction));
                        }
                    }
                }
            }

            state.maxSpeed = maxSpeed;
            state.accelerationRate = accelerationRate;
            state.dragCoefficient = dragCoefficient;

            // Cache built model once per unique ResourceId (renderer-level cache).
            // Generated geometry never changes at runtime, so no invalidation needed.
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                BlockStateModel cached = modelsCache.get(modelInfo.modelId);
                if (cached == null) {
                    long tp = debugRender ? System.nanoTime() : 0;
                    cached = buildModel(modelInfo.modelId);
                    modelsCache.put(modelInfo.modelId, cached);
                    if (debugRender) profiler.recordCollectParts(tp);
                }
                modelInfo.model = cached;
            }
        }

        // Extract traveling items. Each item render state is frame-local and loader-neutral.
        state.travelingItems.clear();
        for (TravelingItem travelingItem : entity.getTravelingItems()) {
            ItemStackRenderState cached = new ItemStackRenderState();
            long ta = debugRender ? System.nanoTime() : 0;
            this.itemModelManager.appendItemLayers(
                    cached,
                    travelingItem.getStack(),
                    ItemDisplayContext.GROUND,
                    entity.getLevel(),
                    null,
                    0);
            if (debugRender) profiler.recordAppendItemLayers(ta);
            TravelingItemRenderState itemState = new TravelingItemRenderState(cached);

            // Position data must be updated every frame for smooth animation
            itemState.direction = travelingItem.getDirection();
            itemState.progress = travelingItem.getProgress();
            itemState.currentSpeed = travelingItem.getSpeed();
            itemState.yOffset = travelingItem.getStack().getItem() instanceof BlockItem ? BLOCK_OFFSET : ITEM_OFFSET;

            state.travelingItems.add(itemState);
        }

        if (debugRender) profiler.endExtract(t0);
    }

    /** Build a ModelRenderInfo for cores/decorations. Parts are populated in extractRenderState(). */
    private static PipeRenderState.ModelRenderInfo buildModelInfo(ResourceId modelId, int color) {
        return new PipeRenderState.ModelRenderInfo(modelId, color);
    }

    /** Build a ModelRenderInfo for arm models that need direction-based rotation. */
    private static PipeRenderState.ModelRenderInfo buildModelInfo(ResourceId modelId, int color, Direction direction) {
        return new PipeRenderState.ModelRenderInfo(modelId, color, direction);
    }

    @Override
    public void submit(
            PipeRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {

        boolean debugRender = DebugLog.isEnabled("render");
        long t0 = debugRender ? profiler.startSubmit() : 0;

        if (!state.models.isEmpty()) {
            RenderType renderLayer = state.blockState == null
                    ? RenderTypes.cutoutMovingBlock()
                    : ItemBlockRenderTypes.getRenderType(state.blockState);
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                // Use cached model — no repeated lookup here
                if (modelInfo.model == null) {
                    continue;
                }

                // Apply rotation for arm models
                if (modelInfo.armDirection != null) {
                    matrices.pushPose();
                    matrices.translate(0.5, 0.5, 0.5); // Rotate around block center
                    applyDirectionRotation(matrices, modelInfo.armDirection);
                    matrices.translate(-0.5, -0.5, -0.5);
                }

                int color = modelInfo.color;
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                queue.submitBlockModel(
                        matrices,
                        renderLayer,
                        modelInfo.model,
                        red,
                        green,
                        blue,
                        state.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        0);

                if (modelInfo.armDirection != null) {
                    matrices.popPose();
                }
            }
        }

        for (TravelingItemRenderState itemState : state.travelingItems) {
            matrices.pushPose();

            // Calculate speed change during this partial tick
            float speedChange = 0f;
            boolean deceleratingToMax = itemState.currentSpeed > state.maxSpeed;
            if (deceleratingToMax) {
                float remaining = Math.max(1.0e-4f, 1.0f - itemState.progress);
                float targetSquared = state.maxSpeed * state.maxSpeed;
                float currentSquared = itemState.currentSpeed * itemState.currentSpeed;
                float decel = (targetSquared - currentSquared) / (2.0f * remaining);
                speedChange = decel * state.tickDelta;
            } else if (state.accelerationRate != 0f) {
                speedChange = state.accelerationRate * state.tickDelta;
            } else if (state.dragCoefficient != 0f) {
                speedChange = -(itemState.currentSpeed * state.dragCoefficient) * state.tickDelta;
            }

            // Speed at the end of this partial tick
            float minSpeed = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED);
            float interpolatedSpeed = itemState.currentSpeed + speedChange;
            if (interpolatedSpeed < minSpeed) {
                interpolatedSpeed = minSpeed;
            } else if (!deceleratingToMax && interpolatedSpeed > state.maxSpeed) {
                interpolatedSpeed = state.maxSpeed;
            }

            // Use average speed for progress calculation (trapezoidal integration)
            float avgSpeed = (itemState.currentSpeed + interpolatedSpeed) / 2.0f;
            float interpolatedProgress = itemState.progress + (avgSpeed * state.tickDelta);

            // Start at center of pipe. The Y offset accounts for the GROUND display transform's
            // built-in upward translation, which differs between block items and regular items.
            matrices.translate(0.5, itemState.yOffset, 0.5);

            // Calculate position along the travel direction
            // Progress 0.0 = entering from opposite direction (-0.5)
            // Progress 1.0 = exiting in travel direction (+0.5)
            float travelDistance = interpolatedProgress - 0.5f;
            matrices.translate(
                    itemState.direction.getStepX() * travelDistance,
                    itemState.direction.getStepY() * travelDistance,
                    itemState.direction.getStepZ() * travelDistance);

            // Keep items at ground scale (no scaling)
            // ItemDisplayContext.GROUND already handles proper item sizing
            // No rotation - items move straight through the pipe

            // Render the item using ItemRenderState.render()
            itemState.itemRenderState.submit(
                    matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0 // outlineColors
                    );

            matrices.popPose();
        }

        if (debugRender) profiler.endSubmit(t0);
    }

    /**
     * Applies rotation to the matrix stack for rendering arm models in the given direction.
     * The base arm model is oriented NORTH; this method rotates it to face other directions.
     */
    private static void applyDirectionRotation(PoseStack matrices, Direction direction) {
        switch (direction) {
            case SOUTH -> matrices.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> matrices.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> matrices.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> matrices.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> matrices.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {} // NORTH: Base orientation, no rotation
        }
    }

    /**
     * Minimal {@link BlockStateModel} that yields pre-built parts. On 1.21.11 {@code submitBlockModel}
     * takes a model (not a part list), so the procedural parts are wrapped here; {@code collectParts}
     * is deterministic and ignores the random source.
     */
    private record ProceduralModel(List<BlockModelPart> parts, TextureAtlasSprite particle) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> out) {
            out.addAll(parts);
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return particle;
        }
    }
}
