package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

public final class LogisticsFabric implements ModInitializer {
    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();

    @Override
    public void onInitialize() {
        LogisticsMod.LOGGER.info("Initializing {}", LogisticsMod.MOD_ID);
        COMMON_BOOTSTRAP.initialize();

        FabricChestLootModifier.register();
        FabricNetworkTickHandler.register();
        FabricCommandRegistration.register();
        FabricServerLevelEvents.register();
        FabricPacketRegistration.register();

        FabricLoader.getInstance().getModContainer(LogisticsMod.MOD_ID).ifPresent(container ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                LogisticsMod.modId("classic_crafting").toIdentifier(),
                container,
                ResourcePackActivationType.NORMAL
            )
        );
    }
}
