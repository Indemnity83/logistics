package com.logistics;

import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.automation.render.LaserQuarryRenderState;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var entry : MODEL.getAllModels()) {
                pluginContext.addModel(entry.getKey(), SimpleUnbakedExtraModel.blockStateModel(entry.getValue().toIdentifier()));
            }
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsAutomation
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering automation (client)");
        BlockRenderLayerMap.putBlock(LogisticsAutomation.BLOCK.MARKER, ChunkSectionLayer.CUTOUT);
        // Register quarry frame for cutout rendering (transparency support)
        BlockRenderLayerMap.putBlock(LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME, ChunkSectionLayer.CUTOUT);

        BlockEntityRendererRegistry.register(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntityRenderer::new);

        MenuScreens.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);

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
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(LogisticsAutomation::model);

        public static final ExtraModelKey<BlockStateModel> ARM = REGISTRY.registerModel("laser_quarry_gantry_arm");
        public static final ExtraModelKey<BlockStateModel> DRILL = REGISTRY.registerModel("laser_quarry_drill");
        public static final ExtraModelKey<BlockStateModel> LED_GREEN = REGISTRY.registerModel("laser_quarry_led_green");
        public static final ExtraModelKey<BlockStateModel> LED_RED = REGISTRY.registerModel("laser_quarry_led_red");
        public static final ExtraModelKey<BlockStateModel> DISPLAY = REGISTRY.registerModel("laser_quarry_display");
        public static final ExtraModelKey<BlockStateModel> TOP_HATCH = REGISTRY.registerModel("laser_quarry_top_hatch");

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}
