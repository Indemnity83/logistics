package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.render.model.CableUnbakedRoot;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
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

        // Register engine block entity renderers
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register cable blocks for cutout rendering (transparent textures)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.COPPER_CABLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.GOLD_CABLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.ENDER_CABLE, RenderType.cutout());

        // Engines render cutout so the outer trunk's transparent window gaps reveal the heat core
        // drawn behind them in the BER. MC 1.21.1 vanilla doesn't honor the model "render_type"
        // field (added in 1.21.4+), so Fabric needs this explicit registration; NeoForge picks up
        // "render_type" from the static model JSON instead.
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.REDSTONE_ENGINE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.STIRLING_ENGINE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPower.BLOCK.CREATIVE_ENGINE, RenderType.cutout());

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Engine heat tinting is rendered in EngineBlockEntityRenderer (the heat core is drawn and
        // tinted per-frame from the live STAGE), so no BlockColors provider is needed here.

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }
}
