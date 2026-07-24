package com.logistics.power.render;

import com.logistics.core.lib.client.render.MachineModels;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.EngineHeatTint;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Renders engine block entities with animated pistons and bellows.
 * Uses code-generated models with transformations for smooth animations.
 * Uses client-side animation cache for smooth piston movement independent of server updates.
 */
public class EngineBlockEntityRenderer implements BlockEntityRenderer<EngineEntity, EngineRenderState> {
    // Animation cache - persists between frames, cleaned up when block entities are removed
    private static final java.util.Map<BlockPos, AnimationCache> ANIMATION_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final class AnimationCache {
        float progress = 0f;
        long lastGameTick = -1;
    }

    private static final float DEFAULT_PISTON_SPEED = 0.02f;

    /**
     * Removes the animation cache entry for a block position.
     * Should be called when an engine block entity is removed.
     */
    public static void clearAnimationCache(BlockPos pos) {
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
            EngineEntity entity,
            EngineRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(
                entity, state, crumblingOverlay);

        state.pos = entity.getBlockPos();
        state.facing = entity.getBlockState().getValue(BlockStateProperties.FACING);

        if (entity instanceof RedstoneEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.REDSTONE;
        } else if (entity instanceof StirlingEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.STIRLING;
        } else if (entity instanceof com.logistics.power.engine.block.entity.SteamEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.STEAM;
        } else if (entity instanceof CreativeEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.CREATIVE;
        } else if (entity instanceof FuelEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.FUEL;
        }

        state.isRunning = entity.isRunning();
        state.pistonSpeed = entity.getPistonSpeed();

        // Update animation using persistent cache
        AnimationCache cache = ANIMATION_CACHE.computeIfAbsent(state.pos, k -> new AnimationCache());
        updateAnimationCache(cache, state.pistonSpeed, state.isRunning);
        state.setAnimationProgress(cache.progress);
    }

    @Override
    public void submit(
            EngineRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState) {

        // Get code-generated models
        BlockStateModel bellowModel = MachineModels.model(getBellowKey(state.engineType));
        BlockStateModel pistonModel = MachineModels.model(getPistonKey(state.engineType));
        BlockStateModel coreModel = MachineModels.tintedModel(getCoreKey(state.engineType));

        RenderType renderLayer = ItemBlockRenderTypes.getRenderType(state.blockState);
        int light = state.lightCoords;
        float pistonOffset = state.getPistonOffset();

        // Heat-stage tint for the core, read per-frame from the live block state.
        int heat = EngineHeatTint.color(state.blockState);
        float heatR = (heat >> 16 & 0xFF) / 255.0f;
        float heatG = (heat >> 8 & 0xFF) / 255.0f;
        float heatB = (heat & 0xFF) / 255.0f;

        matrices.pushPose();
        applyFacingRotation(matrices, state.facing);

        // Render the heat-colored core (static trunk; tinted per-frame from the live heat stage).
        // Replaces the old static-model tint, which baked a single color for every heat stage.
        queue.submitBlockModel(
                matrices,
                renderLayer,
                coreModel,
                heatR, heatG, heatB,
                light,
                OverlayTexture.NO_OVERLAY,
                0);

        // Render bellow (stretches from Y=4 to piston bottom)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f, 0);
        float bellowScale = Math.max(pistonOffset / 0.5f, 0.01f);
        matrices.scale(1.0f, bellowScale, 1.0f);
        queue.submitBlockModel(
                matrices,
                renderLayer,
                bellowModel,
                1.0f, 1.0f, 1.0f,
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        // Render piston (translates with animation)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f + pistonOffset, 0);
        queue.submitBlockModel(
                matrices,
                renderLayer,
                pistonModel,
                1.0f, 1.0f, 1.0f,
                light,
                OverlayTexture.NO_OVERLAY,
                0);
        matrices.popPose();

        matrices.popPose();
    }

    private String getBellowKey(EngineRenderState.EngineType type) {
        return switch (type) {
            case REDSTONE -> "redstone_engine_bellow";
            case STIRLING -> "stirling_engine_bellow";
            case STEAM -> "steam_engine_bellow";
            case CREATIVE -> "creative_engine_bellow";
            case FUEL -> "fuel_engine_bellow";
        };
    }

    private String getCoreKey(EngineRenderState.EngineType type) {
        return switch (type) {
            case REDSTONE -> "redstone_engine_core";
            case STIRLING -> "stirling_engine_core";
            case STEAM -> "steam_engine_core";
            case CREATIVE -> "creative_engine_core";
            case FUEL -> "fuel_engine_core";
        };
    }

    private String getPistonKey(EngineRenderState.EngineType type) {
        return switch (type) {
            case REDSTONE -> "redstone_engine_piston";
            case STIRLING -> "stirling_engine_piston";
            case STEAM -> "steam_engine_piston";
            case CREATIVE -> "creative_engine_piston";
            case FUEL -> "fuel_engine_piston";
        };
    }

    /**
     * Updates the animation cache with client-side smooth animation.
     * Progress increments based on piston speed and elapsed game ticks.
     */
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

        if (elapsedTicks <= 0) {
            cache.lastGameTick = currentTick;
            return;
        }

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

    /**
     * Applies rotation to face the engine in the correct direction.
     * Engines are modeled to face UP by default.
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
}
