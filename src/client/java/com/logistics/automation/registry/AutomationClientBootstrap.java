package com.logistics.automation.registry;

import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.QuarryBlockEntityRenderer;
import com.logistics.automation.render.QuarryRenderState;
import com.logistics.automation.screen.QuarryScreen;
import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ChunkSectionLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class AutomationClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        ChunkSectionLayerMap.putBlock(AutomationBlocks.QUARRY_FRAME, ChunkSectionLayer.CUTOUT);

        BlockEntityRendererRegistry.register(
                AutomationBlockEntities.QUARRY_BLOCK_ENTITY, QuarryBlockEntityRenderer::new);

        MenuScreens.register(AutomationScreenHandlers.QUARRY, QuarryScreen::new);

        ClientRenderCacheHooks.setQuarryInterpolationClearer(QuarryRenderState::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(QuarryRenderState::clearAllInterpolationCaches);

        ClientTickEvents.END_LEVEL_TICK.register(QuarryRenderState::pruneInterpolationCache);
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }
}
