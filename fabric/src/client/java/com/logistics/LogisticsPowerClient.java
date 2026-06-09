package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.render.CableBlockEntityRenderer;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");

        // Register engine block entity renderers
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Cables render fully in code via the shared vanilla CableBlockEntityRenderer
        // (one BE type covers all tiers; tier is read from the block at render time).
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY, CableBlockEntityRenderer::new);

        // Engines render cutout so the outer trunk's transparent window gaps reveal the heat core
        // drawn behind them in the BER (NeoForge picks this up from "render_type" in the static
        // models). The heat tint itself is applied per-frame in EngineBlockEntityRenderer.
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.REDSTONE_ENGINE, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.STIRLING_ENGINE, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsPower.BLOCK.CREATIVE_ENGINE, ChunkSectionLayer.CUTOUT);

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }
}
