package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.fabric.capability.FabricCapabilityRegistration;
import com.logistics.fabric.energy.EnergyStorageAccess;
import net.fabricmc.api.ModInitializer;

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
            return target != null && target.acceptsEnergy();
        });
    }
}
