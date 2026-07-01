package com.logistics;

import com.logistics.automation.alloysmelter.AlloySmelterScreen;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.sawmill.SawmillScreen;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
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
        // The quarry frame uses a transparent torch-style texture and needs the cutout layer.
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, RenderType.cutout());

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        MenuScreens.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);
        MenuScreens.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);
        MenuScreens.register(LogisticsAutomation.MENU.SAWMILL, SawmillScreen::new);
        MenuScreens.register(LogisticsAutomation.MENU.ALLOY_SMELTER, AlloySmelterScreen::new);

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
