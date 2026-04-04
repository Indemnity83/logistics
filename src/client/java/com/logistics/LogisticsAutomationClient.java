package com.logistics;

import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.MarkerBlockEntityRenderer;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(MODEL.getAllModels());
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.MARKER, RenderType.cutout());
        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.QUARTZ_CRYSTAL, RenderType.translucent());

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        MenuScreens.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);
        MenuScreens.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);

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

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(name -> LogisticsAutomation.model(name).toIdentifier());

        public static final ResourceLocation BEAM = REGISTRY.registerModel("marker_beam");
        public static final ResourceLocation ARM = REGISTRY.registerModel("laser_quarry_gantry_arm");
        public static final ResourceLocation DRILL = REGISTRY.registerModel("laser_quarry_drill");
        public static final ResourceLocation LED_GREEN = REGISTRY.registerModel("laser_quarry_led_green");
        public static final ResourceLocation LED_RED = REGISTRY.registerModel("laser_quarry_led_red");
        public static final ResourceLocation DISPLAY = REGISTRY.registerModel("laser_quarry_display");
        public static final ResourceLocation TOP_HATCH = REGISTRY.registerModel("laser_quarry_top_hatch");

        static ResourceLocation[] getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}
