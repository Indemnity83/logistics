package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;
import com.logistics.core.util.TimingLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.Direction;

public final class AutomationDomainBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        TimingLog.time(LogisticsMod.LOGGER, "AutomationBlocks.initialize", AutomationBlocks::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "AutomationBlockEntities.initialize", AutomationBlockEntities::initialize);
        TimingLog.time(
                LogisticsMod.LOGGER, "AutomationScreenHandlers.initialize", AutomationScreenHandlers::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "AutomationItemGroups.initialize", AutomationItemGroups::initialize);

        AutomationItemGroups.registerProviders();

        // Register pipe connectivity for quarry (only accepts connections from above)
        TimingLog.time(
                LogisticsMod.LOGGER,
                "PipeConnectionRegistry.SIDED.registerForBlockEntity",
                () -> PipeConnectionRegistry.SIDED.registerForBlockEntity(
                        (quarry, direction) -> direction == Direction.UP ? quarry : null,
                        AutomationBlockEntities.LASER_QUARRY_BLOCK_ENTITY));

        ServerLevelEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
    }
}
