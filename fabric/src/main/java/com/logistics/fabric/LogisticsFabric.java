package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.power.cable.CableNetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public final class LogisticsFabric implements ModInitializer {
    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();

    @Override
    public void onInitialize() {
        LogisticsMod.LOGGER.info("Initializing {}", LogisticsMod.MOD_ID);
        COMMON_BOOTSTRAP.initialize();

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
}
