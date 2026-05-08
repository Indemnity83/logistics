package com.logistics.fabric.capability;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.pipe.PipeConnectionLookup;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.fabric.fluids.FabricFluidStorage;
import com.logistics.fabric.storage.FabricItemKey;
import com.logistics.fabric.storage.FabricItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;

public final class FabricCapabilityRegistration {
    private FabricCapabilityRegistration() {}

    public static void register() {
        ItemStorageAccess.register();
        FluidStorageAccess.register();
        // EnergyStorageAccess is registered separately by LogisticsFabric.registerEnergyServices()
        PipeConnectionAccess.register();

        // Wire ItemStorageLookup so common pipe modules can find IItemStorage without importing Fabric API
        ItemStorageLookup.register(
                (world, pos, dir) -> FabricItemStorage.wrap(ItemStorage.SIDED.find(world, pos, dir)));

        // Wire the key factory so common code can construct IItemKey from ItemStack
        ItemStorageLookup.registerKeyFactory(FabricItemKey::of);

        // Wire FluidStorageLookup so common code can find IFluidStorage without importing Fabric API
        FluidStorageLookup.register(
                (world, pos, dir) -> FabricFluidStorage.wrap(FluidStorage.SIDED.find(world, pos, dir)));

        // Quarry: only accepts pipe connections from above
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // Wire PipeConnectionLookup so common PipeBlock can query without importing Fabric API
        PipeConnectionLookup.register(
                (level, pos, dir) -> PipeConnectionRegistry.SIDED.find(level, pos, dir));
    }
}
