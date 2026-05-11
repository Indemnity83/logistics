package com.logistics.neoforge.client.render;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class NeoForgeEngineBlockEntityRenderer
        implements BlockEntityRenderer<AbstractEngineBlockEntity, EngineRenderState> {
    private static final java.util.Map<BlockPos, AnimationCache> ANIMATION_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();
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

        if (entity instanceof RedstoneEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.REDSTONE;
        } else if (entity instanceof StirlingEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.STIRLING;
        } else if (entity instanceof CreativeEngineBlockEntity) {
            state.engineType = EngineRenderState.EngineType.CREATIVE;
        }

        state.isRunning = entity.isRunning();
        state.pistonSpeed = entity.getPistonSpeed();

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
        BlockStateModel pistonModel = getModel(getPistonKey(state.engineType));
        BlockStateModel bellowModel = getModel(getBellowKey(state.engineType));

        if (pistonModel == null || bellowModel == null) {
            return;
        }

        RenderType renderLayer = RenderTypes.cutoutMovingBlock();
        int light = state.lightCoords;
        float pistonOffset = state.getPistonOffset();

        matrices.pushPose();
        applyFacingRotation(matrices, state.facing);

        matrices.pushPose();
        matrices.translate(0, 4 / 16f, 0);
        float bellowScale = Math.max(pistonOffset / 0.5f, 0.01f);
        matrices.scale(1.0f, bellowScale, 1.0f);
        List<BlockStateModelPart> bellowParts = new ArrayList<>();
        bellowModel.collectParts(RandomSource.create(0), bellowParts);
        queue.submitBlockModel(matrices, renderLayer, bellowParts, new int[]{-1}, light, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0, 4 / 16f + pistonOffset, 0);
        List<BlockStateModelPart> pistonParts = new ArrayList<>();
        pistonModel.collectParts(RandomSource.create(0), pistonParts);
        queue.submitBlockModel(matrices, renderLayer, pistonParts, new int[]{-1}, light, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();

        matrices.popPose();
    }

    private ClientModelRegistry.ModelKey getBellowKey(EngineRenderState.EngineType type) {
        return switch (type) {
            case REDSTONE -> REDSTONE_BELLOW;
            case STIRLING -> STIRLING_BELLOW;
            case CREATIVE -> CREATIVE_BELLOW;
        };
    }

    private ClientModelRegistry.ModelKey getPistonKey(EngineRenderState.EngineType type) {
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

    private BlockStateModel getModel(ClientModelRegistry.ModelKey key) {
        return ClientModelRegistry.get(key);
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
