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
        static {
            register("pipe_markings");
            register("stone_transport_pipe_core");
            register("stone_transport_pipe_arm");
            register("stone_transport_pipe_arm_extended");
            register("copper_transport_pipe_core");
            register("copper_transport_pipe_core_exposed");
            register("copper_transport_pipe_core_weathered");
            register("copper_transport_pipe_core_oxidized");
            register("copper_transport_pipe_arm");
            register("copper_transport_pipe_arm_exposed");
            register("copper_transport_pipe_arm_weathered");
            register("copper_transport_pipe_arm_oxidized");
            register("copper_transport_pipe_arm_extended");
            register("copper_transport_pipe_arm_extended_exposed");
            register("copper_transport_pipe_arm_extended_weathered");
            register("copper_transport_pipe_arm_extended_oxidized");
            register("gold_transport_pipe_core");
            register("gold_transport_pipe_core_powered");
            register("gold_transport_pipe_arm");
            register("gold_transport_pipe_arm_powered");
            register("gold_transport_pipe_arm_extended");
            register("gold_transport_pipe_arm_extended_powered");
            register("item_extractor_pipe_core");
            register("item_extractor_pipe_arm");
            register("item_extractor_pipe_arm_extended");
            register("item_extractor_pipe_feature");
            register("item_extractor_pipe_feature_extended");
            register("item_filter_pipe_core");
            register("item_filter_pipe_arm");
            register("item_filter_pipe_arm_extended");
            register("item_insertion_pipe_core");
            register("item_insertion_pipe_arm");
            register("item_insertion_pipe_arm_extended");
            register("item_merger_pipe_core");
            register("item_merger_pipe_arm");
            register("item_merger_pipe_arm_extended");
            register("item_merger_pipe_feature");
            register("item_merger_pipe_feature_extended");
            register("item_passthrough_pipe_core");
            register("item_passthrough_pipe_arm");
            register("item_passthrough_pipe_arm_extended");
            register("item_void_pipe_core");
            register("item_void_pipe_arm");
            register("item_void_pipe_arm_extended");
            register("basic_logistics_pipe_core");
            register("basic_logistics_pipe_arm");
            register("basic_logistics_pipe_arm_extended");
            register("provider_logistics_pipe_core");
            register("provider_logistics_pipe_arm");
            register("provider_logistics_pipe_arm_extended");
            register("provider_logistics_pipe_feature_extended");
            register("requester_logistics_pipe_core");
            register("requester_logistics_pipe_arm");
            register("requester_logistics_pipe_arm_extended");
            register("requester_logistics_pipe_feature");
            register("requester_logistics_pipe_feature_extended");
            register("supplier_logistics_pipe_core");
            register("supplier_logistics_pipe_arm");
            register("supplier_logistics_pipe_arm_extended");
            register("supplier_logistics_pipe_feature");
            register("supplier_logistics_pipe_feature_extended");
            register("crafting_logistics_pipe_core");
            register("crafting_logistics_pipe_arm");
            register("crafting_logistics_pipe_arm_extended");
            register("crafting_logistics_pipe_feature_extended");
            register("process_logistics_pipe_core");
            register("process_logistics_pipe_arm");
            register("process_logistics_pipe_arm_extended");
            register("satellite_logistics_pipe_core");
            register("satellite_logistics_pipe_arm");
            register("satellite_logistics_pipe_arm_extended");
            register("chassis_logistics_pipe_mk1_core");
            register("chassis_logistics_pipe_mk1_arm");
            register("chassis_logistics_pipe_mk1_arm_extended");
            register("chassis_logistics_pipe_mk2_core");
            register("chassis_logistics_pipe_mk2_arm");
            register("chassis_logistics_pipe_mk2_arm_extended");
            register("chassis_logistics_pipe_mk3_core");
            register("chassis_logistics_pipe_mk3_arm");
            register("chassis_logistics_pipe_mk3_arm_extended");
            register("chassis_logistics_pipe_mk4_core");
            register("chassis_logistics_pipe_mk4_arm");
            register("chassis_logistics_pipe_mk4_arm_extended");
            register("chassis_logistics_pipe_mk5_core");
            register("chassis_logistics_pipe_mk5_arm");
            register("chassis_logistics_pipe_mk5_arm_extended");
        }

        private static void register(String name) {
            ClientModelRegistry.register(LogisticsPipe.model(name));
        }

        private MODEL() {}
    }
}
