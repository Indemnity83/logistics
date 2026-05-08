package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.core.DebugLog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");
        MODEL.init();

        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

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

    /** Registers all pipe extra models into {@link ClientModelRegistry}. */
    public static final class MODEL {
        private static final String[] PIPE_MODELS = {
            "pipe_markings",
            "stone_transport_pipe_core",
            "stone_transport_pipe_arm",
            "stone_transport_pipe_arm_extended",
            "copper_transport_pipe_core",
            "copper_transport_pipe_core_exposed",
            "copper_transport_pipe_core_weathered",
            "copper_transport_pipe_core_oxidized",
            "copper_transport_pipe_arm",
            "copper_transport_pipe_arm_exposed",
            "copper_transport_pipe_arm_weathered",
            "copper_transport_pipe_arm_oxidized",
            "copper_transport_pipe_arm_extended",
            "copper_transport_pipe_arm_extended_exposed",
            "copper_transport_pipe_arm_extended_weathered",
            "copper_transport_pipe_arm_extended_oxidized",
            "gold_transport_pipe_core",
            "gold_transport_pipe_core_powered",
            "gold_transport_pipe_arm",
            "gold_transport_pipe_arm_powered",
            "gold_transport_pipe_arm_extended",
            "gold_transport_pipe_arm_extended_powered",
            "item_extractor_pipe_core",
            "item_extractor_pipe_arm",
            "item_extractor_pipe_arm_extended",
            "item_extractor_pipe_feature",
            "item_extractor_pipe_feature_extended",
            "item_filter_pipe_core",
            "item_filter_pipe_arm",
            "item_filter_pipe_arm_extended",
            "item_insertion_pipe_core",
            "item_insertion_pipe_arm",
            "item_insertion_pipe_arm_extended",
            "item_merger_pipe_core",
            "item_merger_pipe_arm",
            "item_merger_pipe_arm_extended",
            "item_merger_pipe_feature",
            "item_merger_pipe_feature_extended",
            "item_passthrough_pipe_core",
            "item_passthrough_pipe_arm",
            "item_passthrough_pipe_arm_extended",
            "item_void_pipe_core",
            "item_void_pipe_arm",
            "item_void_pipe_arm_extended",
            "basic_logistics_pipe_core",
            "basic_logistics_pipe_arm",
            "basic_logistics_pipe_arm_extended",
            "provider_logistics_pipe_core",
            "provider_logistics_pipe_arm",
            "provider_logistics_pipe_arm_extended",
            "provider_logistics_pipe_feature_extended",
            "requester_logistics_pipe_core",
            "requester_logistics_pipe_arm",
            "requester_logistics_pipe_arm_extended",
            "requester_logistics_pipe_feature",
            "requester_logistics_pipe_feature_extended",
            "supplier_logistics_pipe_core",
            "supplier_logistics_pipe_arm",
            "supplier_logistics_pipe_arm_extended",
            "supplier_logistics_pipe_feature",
            "supplier_logistics_pipe_feature_extended",
            "crafting_logistics_pipe_core",
            "crafting_logistics_pipe_arm",
            "crafting_logistics_pipe_arm_extended",
            "crafting_logistics_pipe_feature_extended",
            "process_logistics_pipe_core",
            "process_logistics_pipe_arm",
            "process_logistics_pipe_arm_extended",
            "satellite_logistics_pipe_core",
            "satellite_logistics_pipe_arm",
            "satellite_logistics_pipe_arm_extended",
            "chassis_logistics_pipe_mk1_core",
            "chassis_logistics_pipe_mk1_arm",
            "chassis_logistics_pipe_mk1_arm_extended",
            "chassis_logistics_pipe_mk2_core",
            "chassis_logistics_pipe_mk2_arm",
            "chassis_logistics_pipe_mk2_arm_extended",
            "chassis_logistics_pipe_mk3_core",
            "chassis_logistics_pipe_mk3_arm",
            "chassis_logistics_pipe_mk3_arm_extended",
            "chassis_logistics_pipe_mk4_core",
            "chassis_logistics_pipe_mk4_arm",
            "chassis_logistics_pipe_mk4_arm_extended",
            "chassis_logistics_pipe_mk5_core",
            "chassis_logistics_pipe_mk5_arm",
            "chassis_logistics_pipe_mk5_arm_extended",
        };

        static {
            for (String name : PIPE_MODELS) register(name);
        }

        private static void register(String name) {
            ClientModelRegistry.register(LogisticsPipe.model(name));
        }

        /** Forces this class to load, triggering static model registration. */
        public static void init() {}

        private MODEL() {}
    }
}
