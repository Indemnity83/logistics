package com.logistics.neoforge;

// TODO(multiloader): Register packet payloads for NeoForge networking.
// NeoForge equivalent of FabricPacketRegistration.
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.MOD)
// public class NeoForgePacketRegistration {
//
//     @SubscribeEvent
//     public static void registerPayloads(RegisterPayloadHandlersEvent event) {
//         PayloadRegistrar registrar = event.registrar("1");
//
//         registrar.playToServer(
//             RequestItemPacket.TYPE,
//             RequestItemPacket.CODEC,
//             (packet, ctx) -> ctx.enqueueWork(() -> packet.handle(ctx.player()))
//         );
//
//         registrar.playToServer(
//             SetSatelliteIdPacket.TYPE,
//             SetSatelliteIdPacket.CODEC,
//             (packet, ctx) -> ctx.enqueueWork(() -> packet.handle(ctx.player()))
//         );
//
//         registrar.playToServer(
//             OpenChassisSlotPacket.TYPE,
//             OpenChassisSlotPacket.CODEC,
//             (packet, ctx) -> ctx.enqueueWork(() -> packet.handle(ctx.player()))
//         );
//
//         registrar.playToClient(
//             SyncRequesterInventoryPacket.TYPE,
//             SyncRequesterInventoryPacket.CODEC,
//             (packet, ctx) -> { /* client receiver registered in LogisticsPipeClient */ }
//         );
//     }
// }
public final class NeoForgePacketRegistration {
    private NeoForgePacketRegistration() {}
}
