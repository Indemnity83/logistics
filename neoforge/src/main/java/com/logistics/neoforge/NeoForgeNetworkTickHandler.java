package com.logistics.neoforge;

import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.power.cable.CableNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class NeoForgeNetworkTickHandler {
    private NeoForgeNetworkTickHandler() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeNetworkTickHandler::onServerTick);
        gameBus.addListener(NeoForgeNetworkTickHandler::onLevelUnload);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        NetworkRegistry.tickNetworks(event.getServer());
        CableNetworkManager.tickAll(event.getServer());
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NetworkRegistry.clearLevel(level);
            CableNetworkManager.clearLevel(level);
        }
    }
}
