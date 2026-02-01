package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.core.util.TimingLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Direction;

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
                "PipeConnection.SIDED.registerForBlocks",
                () -> PipeConnection.SIDED.registerForBlocks(
                        (world, pos, state, blockEntity, direction) -> direction == Direction.UP ? Unit.INSTANCE : null,
                        AutomationBlocks.LASER_QUARRY));

        ServerWorldEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
    }
}
