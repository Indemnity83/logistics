package com.logistics;

import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            ResourceLocation arm = LogisticsAutomation.blockModelIdentifier("laser_quarry_gantry_arm");
            ResourceLocation drill = LogisticsAutomation.blockModelIdentifier("laser_quarry_drill");
            ResourceLocation ledGreen = LogisticsAutomation.blockModelIdentifier("laser_quarry_led_green");
            ResourceLocation ledRed = LogisticsAutomation.blockModelIdentifier("laser_quarry_led_red");
            ResourceLocation display = LogisticsAutomation.blockModelIdentifier("laser_quarry_display");
            ResourceLocation topHatch = LogisticsAutomation.blockModelIdentifier("laser_quarry_top_hatch");

            pluginContext.addModels(arm, drill, ledGreen, ledRed, display, topHatch);
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        // Register quarry blocks for cutout rendering (transparency support)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, RenderType.cutout());

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        // No screen handler for laser quarry (no GUI)

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
