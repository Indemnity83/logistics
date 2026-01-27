package com.logistics.automation.registry;

import com.logistics.client.ClientRenderCacheHooks;
import com.logistics.client.render.QuarryBlockEntityRenderer;
import com.logistics.client.render.QuarryRenderState;
import com.logistics.client.screen.QuarryScreen;
import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class AutomationClientBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        BlockRenderLayerMap.putBlock(AutomationBlocks.QUARRY_FRAME, BlockRenderLayer.CUTOUT);

        BlockEntityRendererFactories.register(
                AutomationBlockEntities.QUARRY_BLOCK_ENTITY, QuarryBlockEntityRenderer::new);

        HandledScreens.register(AutomationScreenHandlers.QUARRY, QuarryScreen::new);

        ClientRenderCacheHooks.setQuarryInterpolationClearer(QuarryRenderState::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(QuarryRenderState::clearAllInterpolationCaches);

        ClientTickEvents.END_WORLD_TICK.register(QuarryRenderState::pruneInterpolationCache);
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }
}
