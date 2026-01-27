package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.quarry.entity.QuarryBlockEntity;
import com.logistics.util.TimingLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public final class AutomationDomainBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        TimingLog.time(LogisticsMod.LOGGER, "AutomationBlocks.initialize", AutomationBlocks::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "AutomationBlockEntities.initialize", AutomationBlockEntities::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "AutomationScreenHandlers.initialize", AutomationScreenHandlers::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "AutomationItemGroups.initialize", AutomationItemGroups::initialize);

        AutomationItemGroups.registerProviders();

        ServerWorldEvents.UNLOAD.register((server, world) -> QuarryBlockEntity.clearActiveQuarries(world));
    }
}
