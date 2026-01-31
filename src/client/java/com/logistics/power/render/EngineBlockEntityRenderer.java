package com.logistics.power.render;

import com.logistics.LogisticsMod;
import com.logistics.core.render.ModelRegistry;
import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.HeatStage;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import static com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.STAGE;

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
    private static final Identifier TRUNK_BASE_MODEL = Identifier.of(LogisticsMod.MOD_ID, "block/power/engine_trunk_base");
    private static final Identifier TRUNK_OVERLAY_MODEL = Identifier.of(LogisticsMod.MOD_ID, "block/power/engine_trunk_overlay");
    private static final Identifier CHAMBER_MODEL = Identifier.of(LogisticsMod.MOD_ID, "block/power/engine_chamber");

    // Per-engine model identifiers (base static and moving have engine-specific textures)
    private static final Identifier REDSTONE_BASE_STATIC = Identifier.of(LogisticsMod.MOD_ID, "block/power/redstone_engine_base_static");
    private static final Identifier REDSTONE_BASE_MOVING = Identifier.of(LogisticsMod.MOD_ID, "block/power/redstone_engine_base_moving");
    private static final Identifier STIRLING_BASE_STATIC = Identifier.of(LogisticsMod.MOD_ID, "block/power/stirling_engine_base_static");
    private static final Identifier STIRLING_BASE_MOVING = Identifier.of(LogisticsMod.MOD_ID, "block/power/stirling_engine_base_moving");
    private static final Identifier CREATIVE_BASE_STATIC = Identifier.of(LogisticsMod.MOD_ID, "block/power/creative_engine_base_static");
    private static final Identifier CREATIVE_BASE_MOVING = Identifier.of(LogisticsMod.MOD_ID, "block/power/creative_engine_base_moving");

    // Stage colors (RGB 0-1 range) for trunk tinting
    private static final float[] COLOR_BLUE = {0.2f, 0.4f, 0.8f};
    private static final float[] COLOR_GREEN = {0.2f, 0.8f, 0.2f};
    private static final float[] COLOR_YELLOW = {0.8f, 0.8f, 0.2f};
    private static final float[] COLOR_RED = {0.8f, 0.2f, 0.2f};
    private static final float[] COLOR_OVERHEAT = {0.1f, 0.1f, 0.1f};

    // Animation cache - persists between frames, cleaned up when block entities are removed
    private static final java.util.Map<net.minecraft.util.math.BlockPos, AnimationCache> ANIMATION_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class AnimationCache {
        float progress = 0f;
        long lastUpdateTime = 0;
    }

    /**
     * Removes the animation cache entry for a block position.
     * Should be called when an engine block entity is removed.
     */
    public static void clearAnimationCache(net.minecraft.util.math.BlockPos pos) {
        ANIMATION_CACHE.remove(pos);
    }

    /**
     * Clears all animation cache entries.
     * Should be called on world unload to prevent memory leaks.
     */
    public static void clearAllAnimationCache() {
        ANIMATION_CACHE.clear();
    }

    public EngineBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void updateRenderState(
            AbstractEngineBlockEntity entity,
            EngineRenderState state,
            float tickDelta,
            Vec3d cameraPos,
            @Nullable net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        net.minecraft.client.render.block.entity.state.BlockEntityRenderState.updateBlockEntityRenderState(
                entity, state, crumblingOverlay);

        state.pos = entity.getPos();
        state.facing = entity.getCachedState().get(Properties.FACING);

        // Get stage from block state (synced automatically) for reliable rendering
        state.stage = entity.getCachedState().get(STAGE);

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

        // Update animation using persistent cache
        AnimationCache cache = ANIMATION_CACHE.computeIfAbsent(state.pos, k -> new AnimationCache());
        updateAnimationCache(cache, state.pistonSpeed, state.isRunning);
        state.setAnimationProgress(cache.progress);
    }

    private static final float DEFAULT_PISTON_SPEED = 0.02f;

    private void updateAnimationCache(AnimationCache cache, float pistonSpeed, boolean isRunning) {
        long currentTime = System.currentTimeMillis();

        if (cache.lastUpdateTime == 0) {
            cache.lastUpdateTime = currentTime;
            return;
        }

        float elapsedTicks = (currentTime - cache.lastUpdateTime) / 50f;

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

        cache.lastUpdateTime = currentTime;
    }

    @Override
    public void render(
            EngineRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {

        // Select per-engine base models
        BlockStateModel baseStaticModel = getBaseStaticModel(state.engineType);
        BlockStateModel baseMovingModel = getBaseMovingModel(state.engineType);
        BlockStateModel trunkBaseModel = ModelRegistry.getModel(TRUNK_BASE_MODEL);
        BlockStateModel trunkOverlayModel = ModelRegistry.getModel(TRUNK_OVERLAY_MODEL);
        BlockStateModel chamberModel = ModelRegistry.getModel(CHAMBER_MODEL);

        if (baseStaticModel == null || baseMovingModel == null || trunkBaseModel == null
                || trunkOverlayModel == null || chamberModel == null) {
            return;
        }

        RenderLayer renderLayer = RenderLayers.cutout();

        // Get lighting from the output side of the engine for better visibility
        // Sampling from inside the block often results in darkness
        int light = WorldRenderer.getLightmapCoordinates(
                net.minecraft.client.MinecraftClient.getInstance().world,
                state.pos);

        // Calculate piston offset (0 to ~0.5 blocks)
        float pistonOffset = state.getPistonOffset();

        // Get tint color for trunk (with visual oscillation for RED stage)
        float[] trunkColor = getStageColor(state.stage, state.getRenderProgress());

        matrices.push();

        // Apply rotation based on facing direction
        applyFacingRotation(matrices, state.facing);

        // 1. Render static base (Y=0-4)
        matrices.push();
        queue.submitBlockStateModel(
                matrices, renderLayer, baseStaticModel,
                1.0f, 1.0f, 1.0f, // No tint
                light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();

        // 2. Render moving base (Y=4+offset to Y=8+offset)
        matrices.push();
        matrices.translate(0, 4/16f + pistonOffset, 0);
        queue.submitBlockStateModel(
                matrices, renderLayer, baseMovingModel,
                1.0f, 1.0f, 1.0f, // No tint
                light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();

        // 3. Render trunk base (Y=4-16, no tint)
        matrices.push();
        matrices.translate(0, 4/16f, 0);
        queue.submitBlockStateModel(
                matrices, renderLayer, trunkBaseModel,
                1.0f, 1.0f, 1.0f, // No tint
                light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();

        // 4. Render trunk overlay (Y=4-16, with stage color tint)
        matrices.push();
        matrices.translate(0, 4/16f, 0);
        queue.submitBlockStateModel(
                matrices, renderLayer, trunkOverlayModel,
                trunkColor[0], trunkColor[1], trunkColor[2], // Apply tint
                light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();

        // 5. Render chamber (Y=4 to Y=4+offset, scaled by progress)
        if (pistonOffset > 0.01f) {
            matrices.push();
            matrices.translate(0, 4/16f, 0);
            // Scale the chamber height based on piston offset
            // The model is 8 pixels tall, we scale it to match pistonOffset (in blocks)
            float chamberScale = pistonOffset / 0.5f; // Normalize to 0-1 range
            matrices.scale(1.0f, chamberScale, 1.0f);
            queue.submitBlockStateModel(
                    matrices, renderLayer, chamberModel,
                    1.0f, 1.0f, 1.0f, // No tint
                    light, OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
        }

        matrices.pop();
    }

    /**
     * Applies rotation to the matrix stack based on the engine's facing direction.
     * Engine models are created facing UP, so we rotate to match the actual facing.
     */
    private void applyFacingRotation(MatrixStack matrices, Direction facing) {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            case NORTH -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90));
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
            default -> {} // UP - default orientation, no rotation needed
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }

    /**
     * Gets the RGB tint color for the trunk based on engine stage and piston position.
     *
     * BuildCraft-style visual oscillation: When in RED stage, the engine visually
     * alternates between YELLOW (during piston expansion, progress < 0.5) and RED
     * (during piston compression, progress >= 0.5). This creates the iconic "breathing"
     * effect without actual stage transitions.
     */
    private float[] getStageColor(HeatStage stage, float progress) {
        // BuildCraft visual oscillation: show YELLOW during expansion when in RED stage
        if (stage == HeatStage.HOT && progress < 0.5f) {
            return COLOR_YELLOW;
        }
        return switch (stage) {
            case COLD -> COLOR_BLUE;
            case COOL -> COLOR_GREEN;
            case WARM -> COLOR_YELLOW;
            case HOT -> COLOR_RED;
            case OVERHEAT -> COLOR_OVERHEAT;
        };
    }

    @Override
    public int getRenderDistance() {
        return 64;
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
