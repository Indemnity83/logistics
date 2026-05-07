package com.logistics.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("logistics")
public final class LogisticsNeoForge {

    public LogisticsNeoForge(IEventBus eventBus) {
        // NeoForge entry point.
        // TODO(multiloader): wire common domain init, energy services, and capability registration
        //   once multiloader-loader is applied (requires common/ to be fully free of Fabric API).
        //   See: EnergyPushService / EnergyPresenceChecker in AbstractEngineBlockEntity / AbstractEngineBlock,
        //        and IEnergyStorage in common/core/lib/energy/ for the contracts to implement here.
    }
}
