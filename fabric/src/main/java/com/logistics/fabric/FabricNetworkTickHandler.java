package com.logistics.fabric;

import com.logistics.core.fluid.CrudeOilEffects;
import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wires Fabric lifecycle events to pipe network tick/unload hooks.
 * Registered during Fabric mod initialization.
 */
public final class FabricNetworkTickHandler {
    private FabricNetworkTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NetworkRegistry::tickNetworks);
        ServerTickEvents.END_SERVER_TICK.register(CrudeOilEffects::tickAll);
        ServerWorldEvents.UNLOAD.register((server, level) -> NetworkRegistry.clearLevel(level));
    }
}
