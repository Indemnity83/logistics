package com.logistics.power;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.util.TimingLog;
import com.logistics.power.registry.PowerBlockEntities;
import com.logistics.power.registry.PowerBlocks;
import com.logistics.power.registry.PowerItemGroups;
import com.logistics.power.registry.PowerItems;
import com.logistics.power.registry.PowerScreenHandlers;

public final class PowerDomainBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        TimingLog.time(LogisticsMod.LOGGER, "PowerBlocks.initialize", PowerBlocks::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PowerBlockEntities.initialize", PowerBlockEntities::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PowerItems.initialize", PowerItems::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PowerScreenHandlers.initialize", PowerScreenHandlers::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PowerItemGroups.initialize", PowerItemGroups::initialize);

        PowerItemGroups.registerProviders();
    }
}
