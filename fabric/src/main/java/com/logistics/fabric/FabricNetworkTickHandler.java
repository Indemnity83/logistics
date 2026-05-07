package com.logistics.fabric;

import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wires Fabric lifecycle events to pipe network tick/unload hooks.
 * Registered during Fabric mod initialization.
 */
public final class FabricNetworkTickHandler {
    private FabricNetworkTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NetworkRegistry::tickNetworks);
        ServerLevelEvents.UNLOAD.register((server, level) -> NetworkRegistry.clearLevel(level));
    }
}
