package com.logistics.neoforge;

// TODO(multiloader): Clear active quarries on level unload.
// NeoForge equivalent of FabricServerLevelEvents.
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.FORGE)
// public final class NeoForgeServerLevelEvents {
//     @SubscribeEvent
//     public static void onLevelUnload(LevelEvent.Unload event) {
//         if (event.getLevel() instanceof ServerLevel level) {
//             LaserQuarryBlockEntity.clearActiveQuarries(level);
//         }
//     }
// }
public final class NeoForgeServerLevelEvents {
    private NeoForgeServerLevelEvents() {}
}
