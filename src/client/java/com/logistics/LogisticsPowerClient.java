package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements DomainBootstrap {
    public LogisticsPowerClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            Identifier redstone_bellow = LogisticsPower.blockModelIdentifier("redstone_engine_bellow");
            Identifier redstone_piston = LogisticsPower.blockModelIdentifier("redstone_engine_piston");
            Identifier stirling_bellow = LogisticsPower.blockModelIdentifier("stirling_engine_bellow");
            Identifier stirling_piston = LogisticsPower.blockModelIdentifier("stirling_engine_piston");
            Identifier creative_bellow = LogisticsPower.blockModelIdentifier("creative_engine_bellow");
            Identifier creative_piston = LogisticsPower.blockModelIdentifier("creative_engine_piston");

            MODEL.REDSTONE_BELLOW = ExtraModelKey.create(redstone_bellow::toString);
            MODEL.REDSTONE_PISTON = ExtraModelKey.create(redstone_piston::toString);
            MODEL.STIRLING_BELLOW = ExtraModelKey.create(stirling_bellow::toString);
            MODEL.STIRLING_PISTON = ExtraModelKey.create(stirling_piston::toString);
            MODEL.CREATIVE_BELLOW = ExtraModelKey.create(creative_bellow::toString);
            MODEL.CREATIVE_PISTON = ExtraModelKey.create(creative_piston::toString);

            pluginContext.addModel(MODEL.REDSTONE_BELLOW, SimpleUnbakedExtraModel.blockStateModel(redstone_bellow));
            pluginContext.addModel(MODEL.REDSTONE_PISTON, SimpleUnbakedExtraModel.blockStateModel(redstone_piston));
            pluginContext.addModel(MODEL.STIRLING_BELLOW, SimpleUnbakedExtraModel.blockStateModel(stirling_bellow));
            pluginContext.addModel(MODEL.STIRLING_PISTON, SimpleUnbakedExtraModel.blockStateModel(stirling_piston));
            pluginContext.addModel(MODEL.CREATIVE_BELLOW, SimpleUnbakedExtraModel.blockStateModel(creative_bellow));
            pluginContext.addModel(MODEL.CREATIVE_PISTON, SimpleUnbakedExtraModel.blockStateModel(creative_piston));
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
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.REDSTONE_ENGINE, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.STIRLING_ENGINE, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.CREATIVE_ENGINE, ChunkSectionLayer.CUTOUT);

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
     */
    private void registerEngineBlockColors() {
        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }

            return switch (state.getValue(AbstractEngineBlockEntity.STAGE)) {
                case COLD -> 0x3366CC;
                case COOL -> 0x33CC33;
                case WARM -> 0xCCCC33;
                case HOT -> 0xCC3333;
                default -> 0x191919;
            };
        },
        LogisticsPower.BLOCK.REDSTONE_ENGINE,
        LogisticsPower.BLOCK.STIRLING_ENGINE,
        LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }
}
