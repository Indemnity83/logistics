package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ChunkSectionLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements DomainBootstrap {
    public LogisticsPowerClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            Identifier redstoneBellow = LogisticsPower.blockModelIdentifier("redstone_engine_bellow");
            Identifier redstonePiston = LogisticsPower.blockModelIdentifier("redstone_engine_piston");
            Identifier stirlingBellow = LogisticsPower.blockModelIdentifier("stirling_engine_bellow");
            Identifier stirlingPiston = LogisticsPower.blockModelIdentifier("stirling_engine_piston");
            Identifier creativeBellow = LogisticsPower.blockModelIdentifier("creative_engine_bellow");
            Identifier creativePiston = LogisticsPower.blockModelIdentifier("creative_engine_piston");

            MODEL.REDSTONE_BELLOW = ExtraModelKey.create(redstoneBellow::toString);
            MODEL.REDSTONE_PISTON = ExtraModelKey.create(redstonePiston::toString);
            MODEL.STIRLING_BELLOW = ExtraModelKey.create(stirlingBellow::toString);
            MODEL.STIRLING_PISTON = ExtraModelKey.create(stirlingPiston::toString);
            MODEL.CREATIVE_BELLOW = ExtraModelKey.create(creativeBellow::toString);
            MODEL.CREATIVE_PISTON = ExtraModelKey.create(creativePiston::toString);

            pluginContext.addModel(MODEL.REDSTONE_BELLOW, SimpleUnbakedExtraModel.blockStateModel(redstoneBellow));
            pluginContext.addModel(MODEL.REDSTONE_PISTON, SimpleUnbakedExtraModel.blockStateModel(redstonePiston));
            pluginContext.addModel(MODEL.STIRLING_BELLOW, SimpleUnbakedExtraModel.blockStateModel(stirlingBellow));
            pluginContext.addModel(MODEL.STIRLING_PISTON, SimpleUnbakedExtraModel.blockStateModel(stirlingPiston));
            pluginContext.addModel(MODEL.CREATIVE_BELLOW, SimpleUnbakedExtraModel.blockStateModel(creativeBellow));
            pluginContext.addModel(MODEL.CREATIVE_PISTON, SimpleUnbakedExtraModel.blockStateModel(creativePiston));
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsPower
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");

        // Register engine blocks for cutout rendering (transparent textures)
        ChunkSectionLayerMap.putBlock(LogisticsPower.BLOCK.REDSTONE_ENGINE, ChunkSectionLayer.CUTOUT);
        ChunkSectionLayerMap.putBlock(LogisticsPower.BLOCK.STIRLING_ENGINE, ChunkSectionLayer.CUTOUT);
        ChunkSectionLayerMap.putBlock(LogisticsPower.BLOCK.CREATIVE_ENGINE, ChunkSectionLayer.CUTOUT);

        // Register engine block entity renderers (static rendering for now)
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Register block color providers for engine heat stage tinting
        registerEngineBlockColors();

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }

    public static final class MODEL {
        public static ExtraModelKey<BlockStateModel> REDSTONE_BELLOW;
        public static ExtraModelKey<BlockStateModel> REDSTONE_PISTON;
        public static ExtraModelKey<BlockStateModel> STIRLING_BELLOW;
        public static ExtraModelKey<BlockStateModel> STIRLING_PISTON;
        public static ExtraModelKey<BlockStateModel> CREATIVE_BELLOW;
        public static ExtraModelKey<BlockStateModel> CREATIVE_PISTON;

        private MODEL() {}
    }

    /**
     * Registers block color providers for engines to tint based on heat stage.
     * Uses the STAGE block state property to determine color.
     *
     * <p>TODO: The non-overheating flash effect (HOT/WARM oscillation) is driven by block state
     * changes on the server tick, which causes chunk rebuilds at each half-stroke transition.
     * The better long-term solution is to render the engine core dynamically in the block entity
     * renderer, where per-frame animation progress is available for smooth, rebuild-free coloring.
     */
    private void registerEngineBlockColors() {
        BlockColorRegistry.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }

            return switch (state.getValue(AbstractEngineBlockEntity.STAGE)) {
                case COLD -> 0x3366CC;
                case COOL -> 0x33CC33;
                case WARM -> 0xCCCC33;
                case HOT -> 0xCC3333;
                case OVERHEAT -> 0x191919;
            };
        },
        LogisticsPower.BLOCK.REDSTONE_ENGINE,
        LogisticsPower.BLOCK.STIRLING_ENGINE,
        LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }
}
