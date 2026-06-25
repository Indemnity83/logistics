package com.logistics.neoforge;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsFluid;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.fluids.FluidContainerInteraction;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.pipe.PipeConnectionLookup;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.lib.tank.TankCell;
import com.logistics.core.lib.tank.TankCellLookup;
import com.logistics.neoforge.energy.NeoForgeEnergyStorage;
import com.logistics.neoforge.fluids.NeoForgeFluidStorage;
import com.logistics.neoforge.storage.NeoForgeItemKey;
import com.logistics.neoforge.storage.NeoForgeItemStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class NeoForgeCapabilityRegistration {
    private NeoForgeCapabilityRegistration() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeCapabilityRegistration::registerCapabilities);

        ItemStorageLookup.register((world, pos, dir) ->
                NeoForgeItemStorage.wrap(world.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir)));
        ItemStorageLookup.registerKeyFactory(NeoForgeItemKey::of);

        FluidStorageLookup.register((world, pos, dir) ->
                NeoForgeFluidStorage.wrap(world.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir)));

        // Wire the cross-mod tank-column lookup: catch any block entity that is itself a TankCell (the
        // Glass Tank), and supply the column-global gas predicate. Other mods append their own finders.
        TankCellLookup.register((world, pos) -> world.getBlockEntity(pos) instanceof TankCell cell ? cell : null);
        TankCellLookup.registerGasPredicate(key -> PlatformService.INSTANCE.isLighterThanAir(key.getFluid()));

        FluidContainerInteraction.register((world, pos, player, hand, side) ->
                net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, world, pos, side)
                        ? net.minecraft.world.InteractionResult.SUCCESS
                        : net.minecraft.world.InteractionResult.PASS);

        PipeConnectionLookup.register((level, pos, direction) -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PipeConnection connection
                    && connection.getConnectionType(direction) != PipeConnection.Type.NONE) {
                return connection;
            }
            return null;
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerEnergy(event, LogisticsAutomation.ENTITY.MACERATOR_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.CREATIVE_SINK_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.BATTERY_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY);
        // Fluid Extractor Pipes receive engine RF; copper pipes return a null energy handler.
        registerEnergy(event, LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY);

        registerItems(event, LogisticsAutomation.ENTITY.MACERATOR_BLOCK_ENTITY);
        registerItems(event, LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY);
        registerItems(event, LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY);
        registerItems(event, LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY);

        // Fluid pipes expose their buffer tank as a fluid handler on every enabled side.
        registerFluids(event, LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY);
        // Glass tanks expose their whole vertical column as a fluid handler.
        registerFluids(event, LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY);
        registerFluids(event, LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY);
    }

    private static <BE extends BlockEntity & HasEnergyStorage> void registerEnergy(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeEnergyStorage.asNeoForge(blockEntity.energyStorage(side)));
    }

    private static <BE extends BlockEntity & HasItemStorage> void registerItems(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeItemStorage.asNeoForge(blockEntity.itemStorage(side)));
    }

    private static <BE extends BlockEntity & HasFluidStorage> void registerFluids(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeFluidStorage.asNeoForge(blockEntity.fluidStorage(side)));
    }
}
