package com.logistics.core.network;

import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Handles ticking of pipe networks.
 * Registered during domain initialization.
 */
public class NetworkTickHandler {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var level : server.getAllLevels()) {
                NetworkRegistry.tickNetworks(level);
            }
        });
        ServerWorldEvents.UNLOAD.register((server, level) -> NetworkRegistry.clearLevel(level));
    }
}
