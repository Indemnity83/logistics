package com.logistics.automation.registry;

import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.LaserQuarryRenderState;
import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class AutomationClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        BlockRenderLayerMap.putBlock(AutomationBlocks.LASER_QUARRY_FRAME, BlockRenderLayer.CUTOUT);

        BlockEntityRendererFactories.register(
                AutomationBlockEntities.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        ClientRenderCacheHooks.setQuarryInterpolationClearer(LaserQuarryRenderState::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(LaserQuarryRenderState::clearAllInterpolationCaches);

        ClientTickEvents.END_WORLD_TICK.register(LaserQuarryRenderState::pruneInterpolationCache);
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }
}
