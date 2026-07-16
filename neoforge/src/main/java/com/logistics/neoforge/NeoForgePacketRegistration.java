package com.logistics.neoforge;

import com.logistics.automation.fabricator.SyncFabricatorOutputsPacket;
import com.logistics.automation.fabricator.ToggleFabricatorSelectionPacket;
import com.logistics.automation.jei.SyncMachineRecipesPacket;
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

public final class NeoForgePacketRegistration {
    private NeoForgePacketRegistration() {}

    public static void register(IEventBus modBus) {
        ServerNetworking.register(PacketDistributor::sendToPlayer);
        modBus.addListener(NeoForgePacketRegistration::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(RequestItemPacket.TYPE, RequestItemPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToServer(SetSatelliteIdPacket.TYPE, SetSatelliteIdPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToServer(OpenChassisSlotPacket.TYPE, OpenChassisSlotPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));
        registrar.playToServer(ToggleFabricatorSelectionPacket.TYPE, ToggleFabricatorSelectionPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle((ServerPlayer) context.player())));

        registrar.playToClient(SyncRequesterInventoryPacket.TYPE, SyncRequesterInventoryPacket.CODEC);
        registrar.playToClient(SyncFabricatorOutputsPacket.TYPE, SyncFabricatorOutputsPacket.CODEC);
        registrar.playToClient(SyncMachineRecipesPacket.TYPE, SyncMachineRecipesPacket.CODEC);
    }
}
