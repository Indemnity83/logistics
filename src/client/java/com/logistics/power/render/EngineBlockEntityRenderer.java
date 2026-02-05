package com.logistics.power.render;

import static com.logistics.core.lib.power.AbstractEngineBlockEntity.STAGE;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.core.render.ModelRegistry;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Renders engine block entities with animated pistons.
 *
 * Engine structure (when facing UP):
 * - Base (static): 16×4×16 at Y=0-4
 * - Base moving: 16×4×16 that moves with progress (Y=4+offset to Y=8+offset)
 * - Trunk: 8×12×8 centered at Y=4-16, tinted by heat stage
 * - Chamber: 10×(variable)×10 that expands from Y=4 based on progress
 */
public class EngineBlockEntityRenderer implements BlockEntityRenderer<AbstractEngineBlockEntity, EngineRenderState> {
    // Shared model identifiers
    private static final Identifier TRUNK_BASE_MODEL =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/engine_trunk_base");
    private static final Identifier TRUNK_OVERLAY_MODEL =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/engine_trunk_overlay");
    private static final Identifier CHAMBER_MODEL = Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/engine_chamber");

    // Per-engine model identifiers (base static and moving have engine-specific textures)
    private static final Identifier REDSTONE_BASE_STATIC =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/redstone_engine_base_static");
    private static final Identifier REDSTONE_BASE_MOVING =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/redstone_engine_base_moving");
    private static final Identifier STIRLING_BASE_STATIC =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/stirling_engine_base_static");
    private static final Identifier STIRLING_BASE_MOVING =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/stirling_engine_base_moving");
    private static final Identifier CREATIVE_BASE_STATIC =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/creative_engine_base_static");
    private static final Identifier CREATIVE_BASE_MOVING =
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/power/creative_engine_base_moving");

    // Stage colors (RGB 0-1 range) for trunk tinting
    private static final float[] COLOR_BLUE = {0.2f, 0.4f, 0.8f};
    private static final float[] COLOR_GREEN = {0.2f, 0.8f, 0.2f};
    private static final float[] COLOR_YELLOW = {0.8f, 0.8f, 0.2f};
    private static final float[] COLOR_RED = {0.8f, 0.2f, 0.2f};
    private static final float[] COLOR_OVERHEAT = {0.1f, 0.1f, 0.1f};

    // Animation cache - persists between frames, cleaned up when block entities are removed
    private static final java.util.Map<net.minecraft.core.BlockPos, AnimationCache> ANIMATION_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final class AnimationCache {
        float progress = 0f;
        long lastGameTick = -1;
    }

    /**
     * Removes the animation cache entry for a block position.
     * Should be called when an engine block entity is removed.
     */
    public static void clearAnimationCache(net.minecraft.core.BlockPos pos) {
        ANIMATION_CACHE.remove(pos);
    }

    /**
     * Clears all animation cache entries.
     * Should be called on world unload to prevent memory leaks.
     */
    public static void clearAllAnimationCache() {
        ANIMATION_CACHE.clear();
    }

    public EngineBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void extractRenderState(
            AbstractEngineBlockEntity entity,
            EngineRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(
                entity, state, crumblingOverlay);

        state.pos = entity.getBlockPos();
        state.facing = entity.getBlockState().getValue(BlockStateProperties.FACING);

        // Get stage from block state (synced automatically) for reliable rendering
        state.stage = entity.getBlockState().getValue(STAGE);

        // Determine engine type
        if (entity instanceof RedstoneEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.REDSTONE;
        } else if (entity instanceof StirlingEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.STIRLING;
        } else if (entity instanceof CreativeEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.CREATIVE;
        }

        state.isRunning = entity.isRunning();
        state.pistonSpeed = entity.getPistonSpeed();
        state.canOverheat = entity.canOverheat();

        // Update animation using persistent cache
        AnimationCache cache = ANIMATION_CACHE.computeIfAbsent(state.pos, k -> new AnimationCache());
        updateAnimationCache(cache, state.pistonSpeed, state.isRunning);
        state.setAnimationProgress(cache.progress);
    }

    private static final float DEFAULT_PISTON_SPEED = 0.02f;

    private void updateAnimationCache(AnimationCache cache, float pistonSpeed, boolean isRunning) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        long currentTick = client.level.getGameTime();

        if (cache.lastGameTick < 0) {
            cache.lastGameTick = currentTick;
            return;
        }

        long elapsedTicks = currentTick - cache.lastGameTick;

        if (isRunning) {
            cache.progress += pistonSpeed * elapsedTicks;
            while (cache.progress >= 1.0f) {
                cache.progress -= 1.0f;
            }
        } else if (cache.progress > 0.001f) {
            // When stopped, finish the cycle back to zero
            float speed = pistonSpeed > 0 ? pistonSpeed : DEFAULT_PISTON_SPEED;

            cache.progress += speed * elapsedTicks;

            // Once we complete the cycle, snap to zero and stop
            if (cache.progress >= 1.0f) {
                cache.progress = 0f;
            }
        }

        cache.lastGameTick = currentTick;
    }

    @Override
    public void submit(
            EngineRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState) {

        // Select per-engine base models
        BlockStateModel baseStaticModel = getBaseStaticModel(state.engineType);
        BlockStateModel baseMovingModel = getBaseMovingModel(state.engineType);
        BlockStateModel trunkBaseModel = ModelRegistry.getModel(TRUNK_BASE_MODEL);
        BlockStateModel trunkOverlayModel = ModelRegistry.getModel(TRUNK_OVERLAY_MODEL);
        BlockStateModel chamberModel = ModelRegistry.getModel(CHAMBER_MODEL);

        if (baseStaticModel == null
                || baseMovingModel == null
                || trunkBaseModel == null
                || trunkOverlayModel == null
                || chamberModel == null) {
            return;
        }

        RenderType renderLayer = RenderTypes.cutoutMovingBlock();

        int light = state.lightCoords;

        // Calculate piston offset (0 to ~0.5 blocks)
        float pistonOffset = state.getPistonOffset();

        // Get tint color for trunk based on heat stage (with oscillation for non-overheating engines)
        float[] trunkColor = getStageColor(state);

        matrices.pushPose();

        // Apply rotation based on facing direction
        applyFacingRotation(matrices, state.facing);

        // 1. Render static base (Y=0-4)
        matrices.pushPose();
        queue.submitBlockModel(
                matrices,
                renderLayer,
                baseStaticModel,
                1.0f,
                1.0f,
                1.0f, // No tint
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        // 2. Render moving base (Y=4+offset to Y=8+offset)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f + pistonOffset, 0);
        queue.submitBlockModel(
                matrices,
                renderLayer,
                baseMovingModel,
                1.0f,
                1.0f,
                1.0f, // No tint
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        // 3. Render trunk base (Y=4-16, no tint)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f, 0);
        queue.submitBlockModel(
                matrices,
                renderLayer,
                trunkBaseModel,
                1.0f,
                1.0f,
                1.0f, // No tint
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        // 4. Render trunk overlay (Y=4-16, with stage color tint)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f, 0);
        queue.submitBlockModel(
                matrices,
                renderLayer,
                trunkOverlayModel,
                trunkColor[0],
                trunkColor[1],
                trunkColor[2], // Apply tint
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        // 5. Render chamber (Y=4 to Y=4+offset, scaled by progress)
        if (pistonOffset > 0.01f) {
            matrices.pushPose();
            matrices.translate(0, 4 / 16f, 0);
            // Scale the chamber height based on piston offset
            // The model is 8 pixels tall, we scale it to match pistonOffset (in blocks)
            float chamberScale = pistonOffset / 0.5f; // Normalize to 0-1 range
            matrices.scale(1.0f, chamberScale, 1.0f);
            queue.submitBlockModel(
                    matrices,
                    renderLayer,
                    chamberModel,
                    1.0f,
                    1.0f,
                    1.0f, // No tint
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0);
            matrices.popPose();
        }

        matrices.popPose();
    }

    /**
     * Applies rotation to the matrix stack based on the engine's facing direction.
     * Engine models are created facing UP, so we rotate to match the actual facing.
     */
    private void applyFacingRotation(PoseStack matrices, Direction facing) {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> matrices.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> matrices.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> matrices.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> matrices.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> matrices.mulPose(Axis.ZP.rotationDegrees(90));
            default -> {} // UP - default orientation, no rotation needed
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }

    /**
     * Gets the RGB tint color for the trunk based on engine stage.
     *
     * <p>For engines that can't overheat, oscillates between RED (expansion, generating)
     * and YELLOW (compression, outputting) when in HOT stage - the iconic "breathing" effect.
     */
    private float[] getStageColor(EngineRenderState state) {
        // Non-overheating engines oscillate: RED during expansion, YELLOW during compression
        if (!state.canOverheat && state.stage == HeatStage.HOT) {
            return state.getRenderProgress() < 0.5f ? COLOR_RED : COLOR_YELLOW;
        }

        return switch (state.stage) {
            case COLD -> COLOR_BLUE;
            case COOL -> COLOR_GREEN;
            case WARM -> COLOR_YELLOW;
            case HOT -> COLOR_RED;
            case OVERHEAT -> COLOR_OVERHEAT;
        };
    }


    /**
     * Gets the base static model for the given engine type.
     */
    private BlockStateModel getBaseStaticModel(EngineRenderState.EngineType engineType) {
        return switch (engineType) {
            case REDSTONE -> ModelRegistry.getModel(REDSTONE_BASE_STATIC);
            case STIRLING -> ModelRegistry.getModel(STIRLING_BASE_STATIC);
            case CREATIVE -> ModelRegistry.getModel(CREATIVE_BASE_STATIC);
        };
    }

    /**
     * Gets the base moving model for the given engine type.
     */
    private BlockStateModel getBaseMovingModel(EngineRenderState.EngineType engineType) {
        return switch (engineType) {
            case REDSTONE -> ModelRegistry.getModel(REDSTONE_BASE_MOVING);
            case STIRLING -> ModelRegistry.getModel(STIRLING_BASE_MOVING);
            case CREATIVE -> ModelRegistry.getModel(CREATIVE_BASE_MOVING);
        };
    }
}
