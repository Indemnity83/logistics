package com.logistics.fabric;

import com.logistics.core.lib.platform.ServerNetworking;
import com.logistics.pipe.network.packet.OpenChassisSlotPacket;
import com.logistics.pipe.network.packet.RequestItemPacket;
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
        PayloadTypeRegistry.serverboundPlay().register(RequestItemPacket.TYPE, RequestItemPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestItemPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.serverboundPlay().register(SetSatelliteIdPacket.TYPE, SetSatelliteIdPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetSatelliteIdPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        PayloadTypeRegistry.serverboundPlay().register(OpenChassisSlotPacket.TYPE, OpenChassisSlotPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenChassisSlotPacket.TYPE,
                (packet, context) -> context.server().execute(() -> packet.handle(context.player())));

        // Clientbound packets (type registration only; receiver registered in LogisticsPipeClient)
        PayloadTypeRegistry.clientboundPlay().register(SyncRequesterInventoryPacket.TYPE, SyncRequesterInventoryPacket.CODEC);
    }
}
