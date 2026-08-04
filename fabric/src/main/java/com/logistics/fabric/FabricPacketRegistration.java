package com.logistics.fabric;

import com.logistics.automation.fabricator.SyncFabricatorOutputsPacket;
import com.logistics.automation.fabricator.ToggleFabricatorSelectionPacket;
import com.logistics.automation.jei.SyncMachineRecipesPacket;
import com.logistics.core.lib.platform.ServerNetworking;
import com.logistics.pipe.network.packet.OpenChassisSlotPacket;
import com.logistics.pipe.network.packet.RequestItemPacket;
import com.logistics.pipe.network.packet.SetFluidSupplierPacket;
import com.logistics.pipe.network.packet.SetSatelliteIdPacket;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricPacketRegistration {
    private FabricPacketRegistration() {}

    public static void register() {
        // Wire common ServerNetworking SPI to Fabric's server-play networking
        ServerNetworking.register(ServerPlayNetworking::send);

        // Serverbound packets
        PayloadTypeRegistry.playC2S().register(RequestItemPacket.TYPE, RequestItemPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestItemPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.playC2S().register(SetSatelliteIdPacket.TYPE, SetSatelliteIdPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetSatelliteIdPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.playC2S().register(SetFluidSupplierPacket.TYPE, SetFluidSupplierPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetFluidSupplierPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.playC2S().register(OpenChassisSlotPacket.TYPE, OpenChassisSlotPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenChassisSlotPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.playC2S().register(
                ToggleFabricatorSelectionPacket.TYPE, ToggleFabricatorSelectionPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleFabricatorSelectionPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        // Clientbound packets (type registration only; receiver registered in client bootstraps)
        PayloadTypeRegistry.playS2C().register(SyncRequesterInventoryPacket.TYPE, SyncRequesterInventoryPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SyncFabricatorOutputsPacket.TYPE, SyncFabricatorOutputsPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SyncMachineRecipesPacket.TYPE, SyncMachineRecipesPacket.CODEC);
    }
}
