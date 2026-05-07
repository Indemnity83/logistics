package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.fabric.capability.FabricCapabilityRegistration;
import com.logistics.fabric.energy.EnergyStorageAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import team.reborn.energy.api.EnergyStorage;

public final class LogisticsFabric implements ModInitializer {
    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();

    @Override
    public void onInitialize() {
        LogisticsMod.LOGGER.info("Initializing {}", LogisticsMod.MOD_ID);
        COMMON_BOOTSTRAP.initialize();

        registerEnergyServices();
        FabricCapabilityRegistration.register();
        FabricChestLootModifier.register();
        FabricNetworkTickHandler.register();
        FabricCommandRegistration.register();
        FabricServerLevelEvents.register();
        FabricPacketRegistration.register();
        FabricBiomeModifications.register();

        FabricLoader.getInstance().getModContainer(LogisticsMod.MOD_ID).ifPresent(container ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                LogisticsMod.modId("classic_crafting").toIdentifier(),
                container,
                ResourcePackActivationType.NORMAL
            )
        );
    }

    private void registerEnergyServices() {
        EnergyStorageAccess.register();

        AbstractEngineBlockEntity.setEnergyPushService((level, targetPos, fromDirection, source, maxAmount) -> {
            EnergyStorage target = EnergyStorage.SIDED.find(level, targetPos, fromDirection);
            if (target == null) return 0L;
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = target.insert(maxAmount, tx);
                if (inserted > 0) tx.commit();
                return inserted;
            }
        });

        AbstractEngineBlock.setEnergyPresenceChecker((world, pos, direction) -> {
            EnergyStorage target = EnergyStorage.SIDED.find(world, pos.relative(direction), direction.getOpposite());
            return target != null && target.supportsInsertion();
        });
    }
}
