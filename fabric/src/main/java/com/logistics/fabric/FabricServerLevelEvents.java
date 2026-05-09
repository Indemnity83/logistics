package com.logistics.fabric;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.power.cable.CableNetworkManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Fabric lifecycle event registrations for automation domain.
 * Registered during Fabric mod initialization.
 */
public final class FabricServerLevelEvents {
    private FabricServerLevelEvents() {}

    public static void register() {
        ServerWorldEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> CableNetworkManager.clearLevel(world));
    }
}
