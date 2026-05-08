package com.logistics.neoforge;

// TODO(multiloader): Register /logistics commands using NeoForge's RegisterCommandsEvent.
// NeoForge equivalent of FabricCommandRegistration.
//
// The command tree is already defined in com.logistics.core.LogisticsCommandTree — only
// the event wiring differs between loaders:
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.FORGE)
// public final class NeoForgeCommandRegistration {
//     @SubscribeEvent
//     public static void onRegisterCommands(RegisterCommandsEvent event) {
//         event.getDispatcher().register(LogisticsCommandTree.build());
//     }
// }
public final class NeoForgeCommandRegistration {
    private NeoForgeCommandRegistration() {}
}
