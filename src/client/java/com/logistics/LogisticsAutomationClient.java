package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        // Public constructor for direct instantiation
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, RenderType.cutout());

        // TODO: Restore laser quarry block entity renderer once rendering is ported to MC 1.21.1
        // BlockEntityRendererRegistry.register(
        //         LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        // No screen handler for laser quarry (no GUI)

        // TODO: Restore render cache cleanup callbacks
        // ClientRenderCacheHooks.setQuarryInterpolationClearer(LaserQuarryRenderState::clearInterpolationCache);
        // ClientRenderCacheHooks.setClearAllInterpolationCaches(LaserQuarryRenderState::clearAllInterpolationCaches);
        // ClientTickEvents.END_CLIENT_TICK.register(client -> {
        //     if (client.level != null) {
        //         LaserQuarryRenderState.pruneInterpolationCache(client.level);
        //     }
        // });
        // ClientPlayConnectionEvents.DISCONNECT.register(
        //         (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        // ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }
}
