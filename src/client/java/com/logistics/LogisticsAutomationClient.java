package com.logistics;

import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.LaserQuarryRenderState;
import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            Identifier arm = LogisticsAutomation.blockModelIdentifier("laser_quarry_gantry_arm");
            Identifier drill = LogisticsAutomation.blockModelIdentifier("laser_quarry_drill");
            Identifier ledGreen = LogisticsAutomation.blockModelIdentifier("laser_quarry_led_green");
            Identifier ledRed = LogisticsAutomation.blockModelIdentifier("laser_quarry_led_red");
            Identifier display = LogisticsAutomation.blockModelIdentifier("laser_quarry_display");
            Identifier topHatch = LogisticsAutomation.blockModelIdentifier("laser_quarry_top_hatch");

            MODEL.ARM = ExtraModelKey.create(arm::toString);
            MODEL.DRILL = ExtraModelKey.create(drill::toString);
            MODEL.LED_GREEN = ExtraModelKey.create(ledGreen::toString);
            MODEL.LED_RED = ExtraModelKey.create(ledRed::toString);
            MODEL.DISPLAY = ExtraModelKey.create(display::toString);
            MODEL.TOP_HATCH = ExtraModelKey.create(topHatch::toString);

            pluginContext.addModel(MODEL.ARM, SimpleUnbakedExtraModel.blockStateModel(arm));
            pluginContext.addModel(MODEL.DRILL, SimpleUnbakedExtraModel.blockStateModel(drill));
            pluginContext.addModel(MODEL.LED_GREEN, SimpleUnbakedExtraModel.blockStateModel(ledGreen));
            pluginContext.addModel(MODEL.LED_RED, SimpleUnbakedExtraModel.blockStateModel(ledRed));
            pluginContext.addModel(MODEL.DISPLAY, SimpleUnbakedExtraModel.blockStateModel(display));
            pluginContext.addModel(MODEL.TOP_HATCH, SimpleUnbakedExtraModel.blockStateModel(topHatch));
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, ChunkSectionLayer.CUTOUT);

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

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

    public static final class MODEL {
        public static ExtraModelKey<BlockStateModel> ARM;
        public static ExtraModelKey<BlockStateModel> DRILL;
        public static ExtraModelKey<BlockStateModel> LED_GREEN;
        public static ExtraModelKey<BlockStateModel> LED_RED;
        public static ExtraModelKey<BlockStateModel> DISPLAY;
        public static ExtraModelKey<BlockStateModel> TOP_HATCH;

        private MODEL() {}
    }
}
