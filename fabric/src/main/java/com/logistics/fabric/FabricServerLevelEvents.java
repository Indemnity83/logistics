package com.logistics.fabric;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.power.cable.CableNetworkManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

/**
 * Fabric lifecycle event registrations for automation domain.
 * Registered during Fabric mod initialization.
 */
public final class FabricServerLevelEvents {
    private FabricServerLevelEvents() {}

    public static void register() {
        ServerLevelEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
        ServerLevelEvents.UNLOAD.register((server, world) -> CableNetworkManager.clearLevel(world));
    }
}
