package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.pipe.client.FluidPacketRendering;
import com.logistics.pipe.client.FluidPacketSpecialRenderer;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.core.DebugLog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import com.logistics.core.lib.compat.ClientScreenCompat;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");

        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        // Fluid is part of the pipe domain; register its client renderers here.
        LogisticsFluidClient.registerClient();

        // Render the fluid-packet item's carried fluid behind its frame's transparent window.
        SpecialModelRenderers.ID_MAPPER.put(
                FluidPacketRendering.ID.toIdentifier(), FluidPacketSpecialRenderer.Unbaked.MAP_CODEC);

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.REQUESTER, com.logistics.pipe.screen.RequesterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SUPPLIER, com.logistics.pipe.screen.SupplierScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.FLUID_SUPPLIER, com.logistics.pipe.screen.FluidSupplierScreen::new);
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
                // Update the requester screen if the payload is addressed to the open menu
                if (ClientScreenCompat.currentScreen() instanceof RequesterScreen requesterScreen) {
                    requesterScreen.applySync(packet);
                }
            });
        });
    }

}
