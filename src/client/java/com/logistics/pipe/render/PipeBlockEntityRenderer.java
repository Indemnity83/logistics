package com.logistics.pipe.render;

import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPipeClient;
import com.logistics.core.DebugLog;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.Pipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.TravelingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

    private final ItemModelResolver itemModelManager;
    private final FabricModelManager modelManager;

    // Model parts never change at runtime — cache globally by ResourceId to avoid collectParts() each frame
    private final Map<ResourceId, List<BlockStateModelPart>> partsCache = new HashMap<>();

    // Per-renderer profiler instance — one profiler per renderer (one renderer per pipe type)
    private final RenderProfiler profiler = new RenderProfiler();

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelManager = ctx.itemModelResolver();
        this.modelManager = (FabricModelManager) Minecraft.getInstance().getModelManager();
    }

    private BlockStateModel getModel(ResourceId modelId) {
        ExtraModelKey<BlockStateModel> key = LogisticsPipeClient.MODEL.getKey(modelId);
        if (key == null) {
            return null;
        }
        BlockStateModel model = modelManager.getModel(key);
        if (model == null) {
            return null;
        }
        return model;
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

        long t0 = DebugLog.isEnabled("render") ? profiler.startExtract() : 0;

        // Update base block entity render state
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        // Store tickDelta for use in render()
        state.tickDelta = tickDelta;

        BlockState blockState = entity.getBlockState();
        state.blockState = blockState;

        state.models.clear();
        {

            // Get pipe properties for speed calculations
            float maxSpeed = LogisticsPipe.CONFIG.PIPE_MAX_SPEED;
            float accelerationRate = 0f;
            float dragCoefficient = LogisticsPipe.CONFIG.DRAG_COEFFICIENT;

            if (blockState.getBlock() instanceof PipeBlock pipeBlock) {
                if (pipeBlock.getPipe() != null && entity.getLevel() != null) {
                    PipeContext context = new PipeContext(entity.getLevel(), entity.getBlockPos(), blockState, entity);
                    maxSpeed = pipeBlock.getPipe().getMaxSpeed(context);
                    accelerationRate = pipeBlock.getPipe().getAccelerationRate(context);
                    dragCoefficient = pipeBlock.getPipe().getDrag(context);

                    Pipe pipe = pipeBlock.getPipe();
                    state.models.add(buildModelInfo(pipe.getCoreModelId(context), 0xFFFFFF));
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
                            state.models.add(buildModelInfo(decoration, 0xFFFFFF));
                        }
                    }
                }
            }

            state.maxSpeed = maxSpeed;
            state.accelerationRate = accelerationRate;
            state.dragCoefficient = dragCoefficient;

            // --- Fix 1: Collect model parts once per unique ResourceId (renderer-level cache) ---
            // Model geometry never changes at runtime, so no invalidation needed.
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                List<BlockStateModelPart> cached = partsCache.get(modelInfo.modelId);
                if (cached == null) {
                    BlockStateModel model = getModel(modelInfo.modelId);
                    if (model == null) {
                        modelInfo.parts = null;
                        continue;
                    }
                    long tp = DebugLog.isEnabled("render") ? System.nanoTime() : 0;
                    cached = new ArrayList<>();
                    model.collectParts(RandomSource.create(0), cached);
                    partsCache.put(modelInfo.modelId, cached);
                    if (DebugLog.isEnabled("render")) profiler.recordCollectParts(tp);
                }
                modelInfo.parts = cached;
            }
        }

        // --- Fix 3: Extract traveling items, reusing cached ItemStackRenderState by ItemVariant ---
        state.itemRenderCache.clear();
        state.travelingItems.clear();
        for (TravelingItem travelingItem : entity.getTravelingItems()) {
            ItemVariant variant = ItemVariant.of(travelingItem.getStack());
            ItemStackRenderState cached = state.itemRenderCache.get(variant);
            if (cached == null) {
                // First time seeing this item type this frame — resolve its model and cache
                cached = new ItemStackRenderState();
                long ta = DebugLog.isEnabled("render") ? System.nanoTime() : 0;
                this.itemModelManager.appendItemLayers(
                        cached,
                        travelingItem.getStack(),
                        ItemDisplayContext.GROUND,
                        entity.getLevel(),
                        null,
                        0);
                if (DebugLog.isEnabled("render")) profiler.recordAppendItemLayers(ta);
                state.itemRenderCache.put(variant, cached);
            }
            TravelingItemRenderState itemState = new TravelingItemRenderState(cached);

            // Position data must be updated every frame for smooth animation
            itemState.direction = travelingItem.getDirection();
            itemState.progress = travelingItem.getProgress();
            itemState.currentSpeed = travelingItem.getSpeed();
            itemState.yOffset = travelingItem.getStack().getItem() instanceof BlockItem ? BLOCK_OFFSET : ITEM_OFFSET;

            state.travelingItems.add(itemState);
        }

        if (DebugLog.isEnabled("render")) profiler.endExtract(t0);
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

        long t0 = DebugLog.isEnabled("render") ? profiler.startSubmit() : 0;

        if (!state.models.isEmpty()) {
            RenderType renderLayer = RenderTypes.cutoutMovingBlock();
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                // Use pre-collected parts (Fix 1) — no collectParts() call here
                if (modelInfo.parts == null) {
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
                queue.submitBlockModel(
                        matrices,
                        renderLayer,
                        modelInfo.parts,
                        new int[]{color | 0xFF000000},
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
            float interpolatedSpeed = itemState.currentSpeed + speedChange;
            if (interpolatedSpeed < LogisticsPipe.CONFIG.ITEM_MIN_SPEED) {
                interpolatedSpeed = LogisticsPipe.CONFIG.ITEM_MIN_SPEED;
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

        if (DebugLog.isEnabled("render")) profiler.endSubmit(t0);
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
}
