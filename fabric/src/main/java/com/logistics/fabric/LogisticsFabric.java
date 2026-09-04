package com.logistics.fabric;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.fabric.capability.FabricCapabilityRegistration;
import com.logistics.fabric.energy.EnergyStorageAccess;
import com.logistics.power.cable.CableNetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public final class LogisticsFabric implements ModInitializer {
    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();

    @Override
    public void onInitialize() {
        LogisticsMod.LOGGER.info("Initializing {}", LogisticsMod.MOD_ID);
        com.logistics.fabric.fluids.FabricFluids.register();
        COMMON_BOOTSTRAP.initialize();
        com.logistics.fabric.fluids.FabricFluids.registerBucketStorage();

        registerEnergyServices();
        FabricCapabilityRegistration.register();
        FabricChestLootModifier.register();
        FabricMobLootModifier.register();
        FabricNetworkTickHandler.register();
        FabricCommandRegistration.register();
        FabricServerLevelEvents.register();
        FabricPlayerJoinEvents.register();
        FabricPacketRegistration.register();
        FabricBiomeModifications.register();

        // Oil-chain and peat fuels; work in furnaces and the Stirling Engine. (Coal is 1600 for reference.)
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(LogisticsCore.ITEM.PEAT, 2000);
            builder.add(LogisticsCore.ITEM.BITUMEN, 3200);
            builder.add(LogisticsCore.ITEM.TAR, 800);
        });

        // Cable network lifecycle events
        ServerTickEvents.END_SERVER_TICK.register(CableNetworkManager::tickAll);
        ServerWorldEvents.UNLOAD.register((server, level) -> CableNetworkManager.clearLevel(level));
    }

    private void registerEnergyServices() {
        EnergyStorageAccess.register();

        EnergyPushService.set((level, targetPos, fromDirection, source, maxAmount) -> {
            IEnergyStorage target = EnergyCapabilityLookup.INSTANCE.find(level, targetPos, fromDirection);
            if (target == null || !target.canInsert()) return 0L;
            // No outer transaction here: it would force the cable network's push to a
            // third-party storage to nest openOuter() inside it, which Fabric forbids.
            return target.insert(maxAmount, false);
        });

        AbstractEngineBlock.setEnergyPresenceChecker((world, pos, direction) -> {
            IEnergyStorage target = EnergyCapabilityLookup.INSTANCE.find(
                    world, pos.relative(direction), direction.getOpposite());
            return target != null && target.canInsert();
        });
    }
}
