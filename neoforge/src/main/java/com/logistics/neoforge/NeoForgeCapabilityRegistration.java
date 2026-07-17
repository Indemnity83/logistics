package com.logistics.neoforge;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
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
import com.logistics.neoforge.fluids.LogisticsBucketFluidHandler;
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
                NeoForgeItemStorage.wrap(world.getCapability(Capabilities.Item.BLOCK, pos, dir)));
        ItemStorageLookup.registerKeyFactory(NeoForgeItemKey::of);

        FluidStorageLookup.register((world, pos, dir) ->
                NeoForgeFluidStorage.wrap(world.getCapability(Capabilities.Fluid.BLOCK, pos, dir)));

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
        registerEnergy(event, LogisticsPipe.ENTITY.POWER_JUNCTION_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.SAWMILL_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.ALLOY_SMELTER_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.CRUCIBLE_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.SEQUENTIAL_FABRICATOR_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.REFINERY_BLOCK_ENTITY);
        registerEnergy(event, LogisticsAutomation.ENTITY.FLUID_PUMP_BLOCK_ENTITY);
        // Extraction pipes (item + fluid) are DirectEnergyReceivers: kept off the energy grid so only
        // a directly-adjacent engine can power them (engines feed them directly).

        registerItems(event, LogisticsAutomation.ENTITY.MACERATOR_BLOCK_ENTITY);
        registerItems(event, LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY);
        registerItems(event, LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY);
        registerItems(event, LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY);
        registerItems(event, LogisticsAutomation.ENTITY.SAWMILL_BLOCK_ENTITY);
        registerItems(event, LogisticsAutomation.ENTITY.ALLOY_SMELTER_BLOCK_ENTITY);
        // Crucible input inventory, so item pipes can insert the melt input.
        registerItems(event, LogisticsAutomation.ENTITY.CRUCIBLE_BLOCK_ENTITY);
        // Fabricator material pool, so item pipes can feed materials in from the top.
        registerItems(event, LogisticsAutomation.ENTITY.SEQUENTIAL_FABRICATOR_BLOCK_ENTITY);

        // Fluid pipes expose their buffer tank as a fluid handler on every enabled side.
        registerFluids(event, LogisticsPipe.ENTITY.FLUID_PIPE_BLOCK_ENTITY);
        // Glass tanks expose their whole vertical column as a fluid handler.
        registerFluids(event, LogisticsPipe.ENTITY.GLASS_TANK_BLOCK_ENTITY);
        registerFluids(event, LogisticsAutomation.ENTITY.FLUID_PUMP_BLOCK_ENTITY);
        // Crucible exposes its output tank as a fluid handler so pipes can pull the product.
        registerFluids(event, LogisticsAutomation.ENTITY.CRUCIBLE_BLOCK_ENTITY);
        // Refinery exposes input+output tanks: pipes fill the input fluid and drain the output.
        registerFluids(event, LogisticsAutomation.ENTITY.REFINERY_BLOCK_ENTITY);

        // Filled buckets expose a one-bucket fluid handler that drains to a plain bucket (Fabric uses
        // FullItemFluidStorage). Empty-bucket -> filled is handled by vanilla BucketResourceHandler via the
        // fluid's getBucket().
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, itemAccess) -> new LogisticsBucketFluidHandler(itemAccess),
                LogisticsCore.BUCKET.all().values().toArray(new net.minecraft.world.item.Item[0]));
    }

    private static <BE extends BlockEntity & HasEnergyStorage> void registerEnergy(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeEnergyStorage.asNeoForge(blockEntity.energyStorage(side)));
    }

    private static <BE extends BlockEntity & HasItemStorage> void registerItems(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeItemStorage.asNeoForge(blockEntity.itemStorage(side)));
    }

    private static <BE extends BlockEntity & HasFluidStorage> void registerFluids(
            RegisterCapabilitiesEvent event,
            BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                type,
                (blockEntity, side) -> NeoForgeFluidStorage.asNeoForge(blockEntity.fluidStorage(side)));
    }
}
