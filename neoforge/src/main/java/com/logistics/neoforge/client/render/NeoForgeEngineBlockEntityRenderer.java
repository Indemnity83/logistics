package com.logistics.neoforge.client.render;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;

public class NeoForgeEngineBlockEntityRenderer
        implements BlockEntityRenderer<AbstractEngineBlockEntity> {
    private static final java.util.Map<BlockPos, AnimationCache> ANIMATION_CACHE =
            new ConcurrentHashMap<>();
    private static final float DEFAULT_PISTON_SPEED = 0.02f;

    private static final ClientModelRegistry.ModelKey REDSTONE_BELLOW =
            ClientModelRegistry.find(LogisticsPower.model("redstone_engine_bellow"));
    private static final ClientModelRegistry.ModelKey REDSTONE_PISTON =
            ClientModelRegistry.find(LogisticsPower.model("redstone_engine_piston"));
    private static final ClientModelRegistry.ModelKey STIRLING_BELLOW =
            ClientModelRegistry.find(LogisticsPower.model("stirling_engine_bellow"));
    private static final ClientModelRegistry.ModelKey STIRLING_PISTON =
            ClientModelRegistry.find(LogisticsPower.model("stirling_engine_piston"));
    private static final ClientModelRegistry.ModelKey CREATIVE_BELLOW =
            ClientModelRegistry.find(LogisticsPower.model("creative_engine_bellow"));
    private static final ClientModelRegistry.ModelKey CREATIVE_PISTON =
            ClientModelRegistry.find(LogisticsPower.model("creative_engine_piston"));

    private static final class AnimationCache {
        float progress = 0f;
        long lastGameTick = -1;
    }

    public static void clearAnimationCache(BlockPos pos) {
        ANIMATION_CACHE.remove(pos);
    }

    public static void clearAllAnimationCache() {
        ANIMATION_CACHE.clear();
    }

    public NeoForgeEngineBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            AbstractEngineBlockEntity entity,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource bufferSource,
            int light,
            int overlay) {
        EngineType type;
        if (entity instanceof RedstoneEngineBlockEntity) {
            type = EngineType.REDSTONE;
        } else if (entity instanceof StirlingEngineBlockEntity) {
            type = EngineType.STIRLING;
        } else if (entity instanceof CreativeEngineBlockEntity) {
            type = EngineType.CREATIVE;
        } else {
            return;
        }

        Direction facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        boolean isRunning = entity.isRunning();
        float pistonSpeed = entity.getPistonSpeed();
        BlockPos pos = entity.getBlockPos();

        AnimationCache cache = ANIMATION_CACHE.computeIfAbsent(pos, k -> new AnimationCache());
        updateAnimationCache(cache, pistonSpeed, isRunning);
        float pistonOffset = computePistonOffset(cache.progress);

        BakedModel pistonModel = ClientModelRegistry.get(getPistonKey(type));
        BakedModel bellowModel = ClientModelRegistry.get(getBellowKey(type));

        if (pistonModel == null || bellowModel == null) {
            return;
        }

        RenderType renderLayer = RenderType.cutout();
        VertexConsumer consumer = bufferSource.getBuffer(renderLayer);
        RandomSource random = RandomSource.create(0);

        matrices.pushPose();
        applyFacingRotation(matrices, facing);

        // Render bellow (scales vertically)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f, 0);
        float bellowScale = Math.max(pistonOffset / 0.5f, 0.01f);
        matrices.scale(1.0f, bellowScale, 1.0f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                matrices.last(), consumer, entity.getBlockState(), bellowModel,
                1.0f, 1.0f, 1.0f, light, overlay, ModelData.EMPTY, renderLayer);
        matrices.popPose();

        // Render piston (translates vertically)
        matrices.pushPose();
        matrices.translate(0, 4 / 16f + pistonOffset, 0);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                matrices.last(), consumer, entity.getBlockState(), pistonModel,
                1.0f, 1.0f, 1.0f, light, overlay, ModelData.EMPTY, renderLayer);
        matrices.popPose();

        matrices.popPose();
    }

    private enum EngineType {
        REDSTONE, STIRLING, CREATIVE
    }

    private ClientModelRegistry.ModelKey getBellowKey(EngineType type) {
        return switch (type) {
            case REDSTONE -> REDSTONE_BELLOW;
            case STIRLING -> STIRLING_BELLOW;
            case CREATIVE -> CREATIVE_BELLOW;
        };
    }

    private ClientModelRegistry.ModelKey getPistonKey(EngineType type) {
        return switch (type) {
            case REDSTONE -> REDSTONE_PISTON;
            case STIRLING -> STIRLING_PISTON;
            case CREATIVE -> CREATIVE_PISTON;
        };
    }

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
            float speed = pistonSpeed > 0 ? pistonSpeed : DEFAULT_PISTON_SPEED;
            cache.progress += speed * elapsedTicks;
            if (cache.progress >= 1.0f) {
                cache.progress = 0f;
            }
        }

        cache.lastGameTick = currentTick;
    }

    private static float computePistonOffset(float progress) {
        float p = progress;
        if (p > 0.5f) {
            p = 1.0f - p;
        }
        return p;
    }

    private void applyFacingRotation(PoseStack matrices, Direction facing) {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> matrices.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> matrices.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> matrices.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> matrices.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> matrices.mulPose(Axis.ZP.rotationDegrees(90));
            default -> {}
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }
}
