package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.render.model.CableUnbakedRoot;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements ClientDomainBootstrap {
    public LogisticsPowerClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.registerBlockStateResolver(LogisticsPower.BLOCK.COPPER_CABLE, ctx -> {
                CableUnbakedRoot root = new CableUnbakedRoot(CableTier.COPPER);
                for (var state : ctx.block().getStateDefinition().getPossibleStates()) {
                    ctx.setModel(state, root);
                }
            });
            pluginContext.registerBlockStateResolver(LogisticsPower.BLOCK.GOLD_CABLE, ctx -> {
                CableUnbakedRoot root = new CableUnbakedRoot(CableTier.GOLD);
                for (var state : ctx.block().getStateDefinition().getPossibleStates()) {
                    ctx.setModel(state, root);
                }
            });
            pluginContext.registerBlockStateResolver(LogisticsPower.BLOCK.ENDER_CABLE, ctx -> {
                CableUnbakedRoot root = new CableUnbakedRoot(CableTier.ENDER);
                for (var state : ctx.block().getStateDefinition().getPossibleStates()) {
                    ctx.setModel(state, root);
                }
            });
        });
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");
        MODEL.init();

        // Register engine block entity renderers
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register cable blocks for cutout rendering (transparent textures)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.COPPER_CABLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.GOLD_CABLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.ENDER_CABLE, RenderType.cutout());

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
        public static final ClientModelRegistry.ModelKey REDSTONE_BELLOW = ClientModelRegistry.register(LogisticsPower.model("redstone_engine_bellow"));
        public static final ClientModelRegistry.ModelKey REDSTONE_PISTON = ClientModelRegistry.register(LogisticsPower.model("redstone_engine_piston"));
        public static final ClientModelRegistry.ModelKey STIRLING_BELLOW = ClientModelRegistry.register(LogisticsPower.model("stirling_engine_bellow"));
        public static final ClientModelRegistry.ModelKey STIRLING_PISTON = ClientModelRegistry.register(LogisticsPower.model("stirling_engine_piston"));
        public static final ClientModelRegistry.ModelKey CREATIVE_BELLOW = ClientModelRegistry.register(LogisticsPower.model("creative_engine_bellow"));
        public static final ClientModelRegistry.ModelKey CREATIVE_PISTON = ClientModelRegistry.register(LogisticsPower.model("creative_engine_piston"));

        /** Forces this class to load, triggering static model registration. */
        public static void init() {}

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
}
