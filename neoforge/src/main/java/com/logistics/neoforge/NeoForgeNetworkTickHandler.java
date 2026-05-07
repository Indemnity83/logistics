package com.logistics.neoforge;

// TODO(multiloader): Tick pipe networks using NeoForge events.
// NeoForge equivalent of FabricNetworkTickHandler.
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.FORGE)
// public final class NeoForgeNetworkTickHandler {
//     @SubscribeEvent
//     public static void onServerTick(TickEvent.ServerTickEvent event) {
//         if (event.phase != TickEvent.Phase.END) return;
//         for (var level : event.getServer().getAllLevels()) {
//             NetworkRegistry.tickNetworks(level);
//         }
//     }
//
//     @SubscribeEvent
//     public static void onLevelUnload(LevelEvent.Unload event) {
//         if (event.getLevel() instanceof ServerLevel level) NetworkRegistry.clearLevel(level);
//     }
// }
public final class NeoForgeNetworkTickHandler {
    private NeoForgeNetworkTickHandler() {}
}
