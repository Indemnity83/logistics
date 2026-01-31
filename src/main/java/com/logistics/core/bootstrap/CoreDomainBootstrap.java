package com.logistics.core.bootstrap;

import com.logistics.LogisticsMod;
import com.logistics.core.registry.CoreBlockEntities;
import com.logistics.core.registry.CoreBlocks;
import com.logistics.core.registry.CoreItemGroups;
import com.logistics.core.registry.CoreItems;
import com.logistics.core.util.TimingLog;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public final class CoreDomainBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        TimingLog.time(LogisticsMod.LOGGER, "CoreItems.initialize", CoreItems::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "CoreBlocks.initialize", CoreBlocks::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "CoreBlockEntities.initialize", CoreBlockEntities::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "CoreItemGroups.initialize", CoreItemGroups::initialize);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> TimingLog.start("server_starting"));
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> TimingLog.logSince(LogisticsMod.LOGGER, "server_starting", "Server starting"));
        ServerWorldEvents.LOAD.register((server, world) -> {
            long start = TimingLog.getStart("server_starting");
            if (start > 0L) {
                TimingLog.log(
                        LogisticsMod.LOGGER,
                        "World load " + world.getRegistryKey().getValue(),
                        start);
            }
        });
    }

    @Override
    public int order() {
        return -100;
    }
}
