package com.logistics.fabric.capability;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.fluids.FluidContainerInteraction;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.pipe.PipeConnectionLookup;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.lib.tank.TankCell;
import com.logistics.core.lib.tank.TankCellLookup;
import com.logistics.fabric.fluids.FabricFluidStorage;
import com.logistics.fabric.storage.FabricItemKey;
import com.logistics.fabric.storage.FabricItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;

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

        // Wire the cross-mod tank-column lookup: catch any block entity that is itself a TankCell (the
        // Glass Tank), and supply the column-global gas predicate. Other mods append their own finders.
        TankCellLookup.register((world, pos) -> world.getBlockEntity(pos) instanceof TankCell cell ? cell : null);
        TankCellLookup.registerGasPredicate(key -> PlatformService.INSTANCE.isLighterThanAir(key.getFluid()));

        // Wire bucket/fluid-container interaction (Fabric Transfer API helper) for tanks
        FluidContainerInteraction.register((world, pos, player, hand, side) -> {
            var storage = FluidStorage.SIDED.find(world, pos, side);
            if (storage == null) {
                return InteractionResult.PASS;
            }
            return FluidStorageUtil.interactWithFluidStorage(storage, player, hand)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        });

        // Quarry: only accepts pipe connections from above
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // Wire PipeConnectionLookup so common PipeBlock can query without importing Fabric API
        PipeConnectionLookup.register(
                (level, pos, dir) -> PipeConnectionRegistry.SIDED.find(level, pos, dir));
    }
}
