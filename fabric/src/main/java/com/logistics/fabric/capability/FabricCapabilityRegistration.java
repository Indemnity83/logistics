package com.logistics.fabric.capability;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.pipe.PipeConnectionLookup;
import net.minecraft.core.Direction;

public final class FabricCapabilityRegistration {
    private FabricCapabilityRegistration() {}

    public static void register() {
        ItemStorageAccess.register();
        FluidStorageAccess.register();
        EnergyStorageAccess.register();
        PipeConnectionAccess.register();

        // Quarry: only accepts pipe connections from above
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // Wire PipeConnectionLookup so common PipeBlock can query without importing Fabric API
        PipeConnectionLookup.register(
                (level, pos, dir) -> PipeConnectionRegistry.SIDED.find(level, pos, dir));
    }
}
