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

import java.util.HashMap;
import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsAutomationClient implements DomainBootstrap {
    public LogisticsAutomationClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var entry : MODEL.getAllModels()) {
                pluginContext.addModel(entry.getKey(), SimpleUnbakedExtraModel.blockStateModel(entry.getValue()));
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
        private static final Map<ExtraModelKey<BlockStateModel>, Identifier> MODELS = new HashMap<>();

        private static ExtraModelKey<BlockStateModel> registerModel(String name) {
            Identifier id = LogisticsAutomation.blockModelIdentifier(name);
            ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
            MODELS.put(key, id);
            return key;
        }

        public static final ExtraModelKey<BlockStateModel> ARM = registerModel("laser_quarry_gantry_arm");
        public static final ExtraModelKey<BlockStateModel> DRILL = registerModel("laser_quarry_drill");
        public static final ExtraModelKey<BlockStateModel> LED_GREEN = registerModel("laser_quarry_led_green");
        public static final ExtraModelKey<BlockStateModel> LED_RED = registerModel("laser_quarry_led_red");
        public static final ExtraModelKey<BlockStateModel> DISPLAY = registerModel("laser_quarry_display");
        public static final ExtraModelKey<BlockStateModel> TOP_HATCH = registerModel("laser_quarry_top_hatch");

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, Identifier>> getAllModels() {
            return MODELS.entrySet();
        }

        private MODEL() {}
    }
}
