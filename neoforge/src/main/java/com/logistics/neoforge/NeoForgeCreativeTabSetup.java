package com.logistics.neoforge;

// TODO(multiloader): Register creative tab content for NeoForge.
// NeoForge uses BuildCreativeModeTabContentsEvent instead of Fabric's CreativeModeTabEvents.
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.MOD)
// public class NeoForgeCreativeTabSetup {
//
//     @SubscribeEvent
//     public static void buildContents(BuildCreativeModeTabContentsEvent event) {
//         if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
//             // Add all logistics items here, mirroring the Fabric creative tab population
//             // event.accept(LogisticsCore.ITEM.WRENCH);
//         }
//     }
// }
//
// Note: Biome modifications (tin/apatite ore worldgen) use a data-driven JSON approach
// on NeoForge via BiomeModifier datapacks, rather than the event-based API.
public final class NeoForgeCreativeTabSetup {
    private NeoForgeCreativeTabSetup() {}
}
