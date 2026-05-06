package com.logistics.neoforge;

// TODO(multiloader): Register capabilities for NeoForge.
// NeoForge equivalent of FabricCapabilityRegistration.
//
// @Mod.EventBusSubscriber(modid = "logistics", bus = Mod.EventBusSubscriber.Bus.MOD)
// public final class NeoForgeCapabilityRegistration {
//
//     @SubscribeEvent
//     public static void registerCapabilities(RegisterCapabilitiesEvent event) {
//         // Energy capability — registered for each block entity that implements HasEnergyStorage
//         // event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
//         //     LogisticsPower.ENTITY.SOME_ENGINE_BLOCK_ENTITY,
//         //     (be, side) -> new NeoForgeEnergyStorage(be.getEnergyStorage(side)));
//
//         // Item capability — registered for each block entity that implements HasItemStorage
//         // event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
//         //     LogisticsCore.ENTITY.SOME_MACHINE_BLOCK_ENTITY,
//         //     (be, side) -> new NeoForgeItemHandler(be.itemStorage(side)));
//
//         // Pipe connection lookup — NeoForge equivalent TBD
//     }
// }
public final class NeoForgeCapabilityRegistration {
    private NeoForgeCapabilityRegistration() {}
}
