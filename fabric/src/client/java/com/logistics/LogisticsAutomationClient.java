package com.logistics;

import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.MarkerBlockEntityRenderer;
import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        // Markers and the quarry frame use transparent torch-style textures and need the cutout layer;
        // without it the inactive marker's transparent pixels render as a black cross.
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.MARKER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, RenderType.cutout());

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        MenuScreens.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);
        MenuScreens.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);

        ClientRenderCacheHooks.setQuarryInterpolationClearer(LaserQuarryBlockEntityRenderer::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(LaserQuarryBlockEntityRenderer::clearAllInterpolationCaches);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                LaserQuarryBlockEntityRenderer.pruneInterpolationCache(client.level);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientRenderCacheHooks.clearAllInterpolationCaches());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientRenderCacheHooks.clearAllInterpolationCaches());
    }

}
