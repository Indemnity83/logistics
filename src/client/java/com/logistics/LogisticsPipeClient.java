package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.core.DebugLog;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements ClientDomainBootstrap {
    public LogisticsPipeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(MODEL.getAllModels());
        });
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");

        // Register pipe blocks for cutout rendering (transparent textures)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.STONE_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_PASSTHROUGH_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_MERGER_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.GOLD_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_FILTER_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_VOID_PIPE, RenderType.cutout());

        // Register pipe block entity renderer
        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        registerMarkingFluidColors();

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.REQUESTER, com.logistics.pipe.screen.RequesterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SUPPLIER, com.logistics.pipe.screen.SupplierScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.PROVIDER, com.logistics.pipe.screen.ProviderScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SINK, com.logistics.pipe.screen.SinkScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CRAFTING, com.logistics.pipe.screen.CraftingScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.PROCESS, com.logistics.pipe.screen.ProcessScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SATELLITE, com.logistics.pipe.screen.SatelliteScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK1, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK2, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK3, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK4, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK5, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.ADVANCED_EXTRACTOR, com.logistics.pipe.screen.AdvancedExtractorScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.MOD_SINK, com.logistics.pipe.screen.ModSinkScreen::new);

        DebugLog.register("render");
        registerPacketReceivers();
    }

    private void registerPacketReceivers() {
        // Sync requester inventory from server
        ClientPlayNetworking.registerGlobalReceiver(SyncRequesterInventoryPacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                // Update the requester screen if it's open
                if (Minecraft.getInstance().screen instanceof RequesterScreen requesterScreen) {
                    requesterScreen.updateAvailableItems(packet.pipePos(), packet.items(), packet.amounts());
                }
            });
        });
    }

    /**
     * Tints layer1 (overlay) of the marking fluid item with the corresponding dye color.
     * layer0 (the bottle/container) is left untinted.
     */
    private void registerMarkingFluidColors() {
        Item[] items = java.util.stream.Stream.of(DyeColor.values())
                .map(LogisticsPipe::getMarkingFluidItem)
                .toArray(Item[]::new);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return -1;

            DyeColor color = LogisticsPipe.getMarkingFluidColor(stack);
            return color != null ? (color.getFireworkColor() | 0xFF000000) : -1;
        }, items);
    }

    public static final class MODEL {
        private static final Map<ResourceLocation, ResourceLocation> TEMP_LOOKUP = new HashMap<>();

        private static void registerModel(String name) {
            ResourceLocation id = LogisticsPipe.model(name).toIdentifier();
            TEMP_LOOKUP.put(id, id);
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
            registerModel("crafting_logistics_pipe_core");
            registerModel("crafting_logistics_pipe_arm");
            registerModel("crafting_logistics_pipe_arm_extended");
            registerModel("crafting_logistics_pipe_feature_extended");
            registerModel("process_logistics_pipe_core");
            registerModel("process_logistics_pipe_arm");
            registerModel("process_logistics_pipe_arm_extended");
            registerModel("satellite_logistics_pipe_core");
            registerModel("satellite_logistics_pipe_arm");
            registerModel("satellite_logistics_pipe_arm_extended");
            registerModel("chassis_logistics_pipe_mk1_core");
            registerModel("chassis_logistics_pipe_mk1_arm");
            registerModel("chassis_logistics_pipe_mk1_arm_extended");
            registerModel("chassis_logistics_pipe_mk2_core");
            registerModel("chassis_logistics_pipe_mk2_arm");
            registerModel("chassis_logistics_pipe_mk2_arm_extended");
            registerModel("chassis_logistics_pipe_mk3_core");
            registerModel("chassis_logistics_pipe_mk3_arm");
            registerModel("chassis_logistics_pipe_mk3_arm_extended");
            registerModel("chassis_logistics_pipe_mk4_core");
            registerModel("chassis_logistics_pipe_mk4_arm");
            registerModel("chassis_logistics_pipe_mk4_arm_extended");
            registerModel("chassis_logistics_pipe_mk5_core");
            registerModel("chassis_logistics_pipe_mk5_arm");
            registerModel("chassis_logistics_pipe_mk5_arm_extended");
        }

        private static final Map<ResourceLocation, ResourceLocation> MODEL_LOOKUP = Map.copyOf(TEMP_LOOKUP);

        @Nullable
        public static ResourceLocation getKey(ResourceLocation modelId) {
            return MODEL_LOOKUP.get(modelId);
        }

        static ResourceLocation[] getAllModels() {
            return MODEL_LOOKUP.values().toArray(new ResourceLocation[0]);
        }

        private MODEL() {}
    }
}
