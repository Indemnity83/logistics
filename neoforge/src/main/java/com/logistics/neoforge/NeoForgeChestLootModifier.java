package com.logistics.neoforge;

// TODO(multiloader): Add logistics items to vanilla chest loot tables.
// NeoForge equivalent of FabricChestLootModifier.
//
// Option A: Subscribe to LootTableLoadEvent (simpler, no JSON required):
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.FORGE)
// public final class NeoForgeChestLootModifier {
//     @SubscribeEvent
//     public static void onLootTableLoad(LootTableLoadEvent event) {
//         ResourceLocation name = event.getName();
//         if (name.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) { ... }
//         // etc. — mirror the loot pools defined in FabricChestLootModifier
//     }
// }
//
// Option B: Implement IGlobalLootModifier with a JSON descriptor in
//   resources/META-INF/neoforge.mods.toml and a corresponding loot_modifier JSON.
public final class NeoForgeChestLootModifier {
    private NeoForgeChestLootModifier() {}
}
