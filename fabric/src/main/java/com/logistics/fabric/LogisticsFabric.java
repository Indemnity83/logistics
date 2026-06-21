package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.fabric.capability.FabricCapabilityRegistration;
import com.logistics.fabric.energy.EnergyStorageAccess;
import com.logistics.fabric.energy.FabricEnergyCapabilityLookup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
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

        // Vanilla creative tab entries (rubber items beside slime ball in Ingredients)
        CreativeTabRegistrar.INSTANCE.modifyTab(CreativeModeTabs.INGREDIENTS, editor -> {
            editor.insertAfter(Items.SLIME_BALL, LogisticsPower.ITEM.RUBBER_MIX);
            editor.insertAfter(LogisticsPower.ITEM.RUBBER_MIX, LogisticsPower.ITEM.RUBBER_CHUNK);
        });
    }

    private void registerEnergyServices() {
        EnergyStorageAccess.register();

        EnergyCapabilityLookup lookup = new FabricEnergyCapabilityLookup();
        EnergyPushService.set((level, targetPos, fromDirection, source, maxAmount) -> {
            IEnergyStorage target = lookup.find(level, targetPos, fromDirection);
            if (target == null || !target.canInsert()) return 0L;
            // No outer transaction here: it would force the cable network's push to a
            // third-party storage to nest openOuter() inside it, which Fabric forbids.
            return target.insert(maxAmount, false);
        });

        AbstractEngineBlock.setEnergyPresenceChecker((world, pos, direction) -> {
            EnergyStorage target = EnergyStorage.SIDED.find(world, pos.relative(direction), direction.getOpposite());
            return target != null && target.supportsInsertion();
        });
    }
}
