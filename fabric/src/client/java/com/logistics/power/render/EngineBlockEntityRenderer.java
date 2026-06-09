package com.logistics.power.render;

import com.logistics.core.lib.client.render.CodeModelRenderer;
import com.logistics.core.lib.client.render.MachineModels;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Renders engine block entities with animated pistons and bellows.
 * Uses code-generated quads with transformations for smooth animations.
 * Uses client-side animation cache for smooth piston movement independent of server updates.
 */
public class EngineBlockEntityRenderer implements BlockEntityRenderer<AbstractEngineBlockEntity> {
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
    public void render(
            AbstractEngineBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Direction facing = entity.getBlockState().getValue(BlockStateProperties.FACING);

        // Get code-generated quads
        List<BakedQuad> pistonModel = MachineModels.quads(getPistonKey(entity));
        List<BakedQuad> bellowModel = MachineModels.quads(getBellowKey(entity));

        if (pistonModel.isEmpty() || bellowModel.isEmpty()) {
            return; // Models not loaded yet
        }

        // Update animation using persistent cache
        AnimationCache cache = ANIMATION_CACHE.computeIfAbsent(entity.getBlockPos(), k -> new AnimationCache());
        updateAnimationCache(cache, entity.getPistonSpeed(), entity.isRunning());

        // Calculate piston offset with back-and-forth motion
        // Progress 0.0 → 0.5 → 1.0, offset 0 → max → 0
        float pistonOffset = (0.5f - Math.abs(cache.progress - 0.5f)) * 2f * 0.5f;

        poseStack.pushPose();
        applyFacingRotation(poseStack, facing);

        // Render bellow (stretches from Y=4 to piston bottom)
        poseStack.pushPose();
        poseStack.translate(0, 4 / 16f, 0);
        // Scale bellow height based on piston offset (bellow model is 8 pixels tall, max offset is 0.5 blocks)
        float bellowScale = Math.max(pistonOffset / 0.5f, 0.01f);
        poseStack.scale(1.0f, bellowScale, 1.0f);
        renderModel(entity, bellowModel, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        // Render piston (translates with animation)
        poseStack.pushPose();
        poseStack.translate(0, 4 / 16f + pistonOffset, 0);
        renderModel(entity, pistonModel, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    private String getBellowKey(AbstractEngineBlockEntity entity) {
        String engineName = getEngineName(entity);
        return switch (engineName) {
            case "redstone_engine" -> "redstone_engine_bellow";
            case "stirling_engine" -> "stirling_engine_bellow";
            case "creative_engine" -> "creative_engine_bellow";
            default -> "redstone_engine_bellow";
        };
    }

    private String getPistonKey(AbstractEngineBlockEntity entity) {
        String engineName = getEngineName(entity);
        return switch (engineName) {
            case "redstone_engine" -> "redstone_engine_piston";
            case "stirling_engine" -> "stirling_engine_piston";
            case "creative_engine" -> "creative_engine_piston";
            default -> "redstone_engine_piston";
        };
    }

    /**
     * Gets the engine name from the block's registry key (e.g., "redstone_engine").
     * Strips the "power/" prefix if present.
     */
    private String getEngineName(AbstractEngineBlockEntity entity) {
        ResourceLocation location = BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock());
        String path = location.getPath();
        // Strip "power/" prefix if present
        return path.startsWith("power/") ? path.substring(6) : path;
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
     * Renders code-generated quads at the current transformation.
     *
     * Uses direct quad rendering with the BER-provided packedLight rather than
     * tesselateWithoutAO, which samples light from adjacent blocks in the baked
     * quad's direction. For the piston's DOWN face (baked at Y=0), that would
     * sample from the block below the engine, which is often solid ground (light=0).
     */
    private void renderModel(
            AbstractEngineBlockEntity entity,
            List<BakedQuad> model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        CodeModelRenderer.draw(model, poseStack, buffer, packedLight, packedOverlay);
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
