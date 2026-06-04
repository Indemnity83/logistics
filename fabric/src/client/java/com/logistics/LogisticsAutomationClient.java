package com.logistics;

import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.LaserQuarryRenderState;
import com.logistics.automation.render.MarkerBlockEntityRenderer;
import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.gui.screens.MenuScreens;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, ChunkSectionLayer.CUTOUT);

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        MenuScreens.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);

        ClientRenderCacheHooks.setQuarryInterpolationClearer(LaserQuarryRenderState::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(LaserQuarryRenderState::clearAllInterpolationCaches);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                LaserQuarryRenderState.pruneInterpolationCache(client.level);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }

}
