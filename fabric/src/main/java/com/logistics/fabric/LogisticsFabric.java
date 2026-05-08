package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.fabric.capability.FabricCapabilityRegistration;
import com.logistics.fabric.energy.EnergyStorageAccess;
import com.logistics.power.cable.CableNetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
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

        // Cable network lifecycle events
        ServerTickEvents.END_SERVER_TICK.register(CableNetworkManager::tickAll);
        ServerWorldEvents.UNLOAD.register((server, level) -> CableNetworkManager.clearLevel(level));

        // Vanilla creative tab entries
        addVanillaCreativeTabEntries();
    }

    private static void addVanillaCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.addAfter(Items.SLIME_BALL, LogisticsPower.ITEM.RUBBER_MIX);
            entries.addAfter(LogisticsPower.ITEM.RUBBER_MIX, LogisticsPower.ITEM.RUBBER_CHUNK);
        });
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
