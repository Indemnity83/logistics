package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.render.ModelKeyRegistry;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements ClientDomainBootstrap {
    public LogisticsPowerClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(MODEL.getAllModels());
        });
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");

        // Register engine blocks for cutout rendering (transparent textures)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.REDSTONE_ENGINE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.STIRLING_ENGINE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.CREATIVE_ENGINE, RenderType.cutout());

        // Register engine block entity renderers (static rendering for now)
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Register block color providers for engine heat stage tinting
        registerEngineBlockColors();

        // Register item color providers for engine items (fixed cool blue color)
        registerEngineItemColors();

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(name -> LogisticsPower.model(name).toIdentifier());

        public static final ResourceLocation REDSTONE_BELLOW = REGISTRY.registerModel("redstone_engine_bellow");
        public static final ResourceLocation REDSTONE_PISTON = REGISTRY.registerModel("redstone_engine_piston");
        public static final ResourceLocation STIRLING_BELLOW = REGISTRY.registerModel("stirling_engine_bellow");
        public static final ResourceLocation STIRLING_PISTON = REGISTRY.registerModel("stirling_engine_piston");
        public static final ResourceLocation CREATIVE_BELLOW = REGISTRY.registerModel("creative_engine_bellow");
        public static final ResourceLocation CREATIVE_PISTON = REGISTRY.registerModel("creative_engine_piston");

        static ResourceLocation[] getAllModels() {
            return REGISTRY.getAllModels();
        }

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
        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
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

    /**
     * Registers item color providers for engine items.
     * Items show a fixed cool blue color (no heat stage variation).
     */
    private void registerEngineItemColors() {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }
            return 0x3366CC;
        },
        LogisticsPower.BLOCK.REDSTONE_ENGINE.asItem(),
        LogisticsPower.BLOCK.STIRLING_ENGINE.asItem(),
        LogisticsPower.BLOCK.CREATIVE_ENGINE.asItem());
    }
}
