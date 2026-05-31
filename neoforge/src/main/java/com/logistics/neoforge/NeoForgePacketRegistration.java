package com.logistics.neoforge;

import com.logistics.core.lib.platform.ServerNetworking;
import com.logistics.pipe.network.packet.OpenChassisSlotPacket;
import com.logistics.pipe.network.packet.RequestItemPacket;
import com.logistics.pipe.network.packet.SetSatelliteIdPacket;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class NeoForgePacketRegistration {
    private static volatile Consumer<SyncRequesterInventoryPacket> syncRequesterInventoryHandler = packet -> {};

    private NeoForgePacketRegistration() {}

    public static void register(IEventBus modBus) {
        ServerNetworking.register(PacketDistributor::sendToPlayer);
        modBus.addListener(NeoForgePacketRegistration::registerPayloads);
    }

    public static void registerSyncRequesterInventoryHandler(Consumer<SyncRequesterInventoryPacket> handler) {
        syncRequesterInventoryHandler = handler;
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(RequestItemPacket.TYPE, RequestItemPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToServer(SetSatelliteIdPacket.TYPE, SetSatelliteIdPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToServer(OpenChassisSlotPacket.TYPE, OpenChassisSlotPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToClient(SyncRequesterInventoryPacket.TYPE, SyncRequesterInventoryPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> syncRequesterInventoryHandler.accept(packet)));
    }
}
