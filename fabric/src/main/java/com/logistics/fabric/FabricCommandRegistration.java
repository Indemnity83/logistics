package com.logistics.fabric;

import com.logistics.core.LogisticsCommandTree;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Wires the Fabric command registration event to the shared command tree.
 * Registered during Fabric mod initialization.
 */
public final class FabricCommandRegistration {
    private FabricCommandRegistration() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(LogisticsCommandTree.build()));
    }
}
