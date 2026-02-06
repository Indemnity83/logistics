package com.logistics;

import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.LaserQuarryRenderState;
import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import static com.mojang.text2speech.Narrator.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        // Public constructor for ServiceLoader
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.putBlock(LogisticAutomation.BLOCK.LASER_QUARRY_FRAME, ChunkSectionLayer.CUTOUT);

        BlockEntityRendererRegistry.register(
                LogisticAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        // No screen handler for laser quarry (no GUI)

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
