package com.logistics.fabric;

import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Handles ticking of pipe networks.
 * Registered during Fabric mod initialization.
 */
public final class FabricNetworkTickHandler {
    private FabricNetworkTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var level : server.getAllLevels()) {
                NetworkRegistry.tickNetworks(level);
            }
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> NetworkRegistry.clearLevel(level));
    }
}
