package com.logistics.fabric;

import com.logistics.LogisticsPipe;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wires Fabric lifecycle events to pipe network tick/unload hooks.
 * Registered during Fabric mod initialization.
 */
public final class FabricNetworkTickHandler {
    private FabricNetworkTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LogisticsPipe::onServerTick);
        ServerLevelEvents.UNLOAD.register((server, level) -> LogisticsPipe.onLevelUnload(level));
    }
}
