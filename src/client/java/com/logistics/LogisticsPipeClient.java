package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements DomainBootstrap {
    public LogisticsPipeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var entry : MODEL.getAllModels()) {
                pluginContext.addModel(entry.getKey(), SimpleUnbakedExtraModel.blockStateModel(entry.getValue().toIdentifier()));
            }
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsPipe
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");

        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.REQUESTER, com.logistics.pipe.screen.RequesterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SUPPLIER, com.logistics.pipe.screen.SupplierScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.PROVIDER, com.logistics.pipe.screen.ProviderScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SINK, com.logistics.pipe.screen.SinkScreen::new);
    }

    public static final class MODEL {
        private static final Map<ResourceId, ExtraModelKey<BlockStateModel>> TEMP_LOOKUP = new HashMap<>();

        private static void registerModel(String name) {
            ResourceId id = LogisticsPipe.model(name);
            ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
            TEMP_LOOKUP.put(id, key);
        }

        static {
            registerModel("pipe_markings");
            registerModel("stone_transport_pipe_core");
            registerModel("stone_transport_pipe_arm");
            registerModel("stone_transport_pipe_arm_extended");
            registerModel("copper_transport_pipe_core");
            registerModel("copper_transport_pipe_core_exposed");
            registerModel("copper_transport_pipe_core_weathered");
            registerModel("copper_transport_pipe_core_oxidized");
            registerModel("copper_transport_pipe_arm");
            registerModel("copper_transport_pipe_arm_exposed");
            registerModel("copper_transport_pipe_arm_weathered");
            registerModel("copper_transport_pipe_arm_oxidized");
            registerModel("copper_transport_pipe_arm_extended");
            registerModel("copper_transport_pipe_arm_extended_exposed");
            registerModel("copper_transport_pipe_arm_extended_weathered");
            registerModel("copper_transport_pipe_arm_extended_oxidized");
            registerModel("gold_transport_pipe_core");
            registerModel("gold_transport_pipe_core_powered");
            registerModel("gold_transport_pipe_arm");
            registerModel("gold_transport_pipe_arm_powered");
            registerModel("gold_transport_pipe_arm_extended");
            registerModel("gold_transport_pipe_arm_extended_powered");
            registerModel("item_extractor_pipe_core");
            registerModel("item_extractor_pipe_arm");
            registerModel("item_extractor_pipe_arm_extended");
            registerModel("item_extractor_pipe_feature");
            registerModel("item_extractor_pipe_feature_extended");
            registerModel("item_filter_pipe_core");
            registerModel("item_filter_pipe_arm");
            registerModel("item_filter_pipe_arm_extended");
            registerModel("item_insertion_pipe_core");
            registerModel("item_insertion_pipe_arm");
            registerModel("item_insertion_pipe_arm_extended");
            registerModel("item_merger_pipe_core");
            registerModel("item_merger_pipe_arm");
            registerModel("item_merger_pipe_arm_extended");
            registerModel("item_merger_pipe_feature");
            registerModel("item_merger_pipe_feature_extended");
            registerModel("item_passthrough_pipe_core");
            registerModel("item_passthrough_pipe_arm");
            registerModel("item_passthrough_pipe_arm_extended");
            registerModel("item_void_pipe_core");
            registerModel("item_void_pipe_arm");
            registerModel("item_void_pipe_arm_extended");
            registerModel("basic_logistics_pipe_core");
            registerModel("basic_logistics_pipe_arm");
            registerModel("basic_logistics_pipe_arm_extended");
            registerModel("provider_logistics_pipe_core");
            registerModel("provider_logistics_pipe_arm");
            registerModel("provider_logistics_pipe_arm_extended");
            registerModel("provider_logistics_pipe_feature_extended");
            registerModel("requester_logistics_pipe_core");
            registerModel("requester_logistics_pipe_arm");
            registerModel("requester_logistics_pipe_arm_extended");
            registerModel("requester_logistics_pipe_feature");
            registerModel("requester_logistics_pipe_feature_extended");
            registerModel("supplier_logistics_pipe_core");
            registerModel("supplier_logistics_pipe_arm");
            registerModel("supplier_logistics_pipe_arm_extended");
            registerModel("supplier_logistics_pipe_feature");
            registerModel("supplier_logistics_pipe_feature_extended");
        }

        private static final Map<ResourceId, ExtraModelKey<BlockStateModel>> MODEL_LOOKUP = Map.copyOf(TEMP_LOOKUP);

        @Nullable
        public static ExtraModelKey<BlockStateModel> getKey(ResourceId modelId) {
            return MODEL_LOOKUP.get(modelId);
        }

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
            return MODEL_LOOKUP.entrySet().stream()
                    .map(e -> Map.entry(e.getValue(), e.getKey()))
                    .toList();
        }

        private MODEL() {}
    }
}
