package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.power.engine.block.FuelEngineBlock;
import com.logistics.power.engine.fuel.FuelEngineComponent;
import com.logistics.power.engine.fuel.FuelEngineCoolants;
import com.logistics.power.engine.fuel.FuelEngineFuels;
import com.logistics.power.engine.fuel.FuelEngineProfile;
import com.logistics.power.engine.ui.FuelEngineScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine: burns liquid fuel (crude oil / bio fuel / fuel oil) with water coolant. Composed of
 * an output buffer, a fuel tank, a coolant tank, and the {@link FuelEngineComponent} simulation, plus a
 * piston cycle for animation + export. Exposes both tanks through one combined fluid view that routes
 * inserts by fluid type.
 */
public class FuelEngineBlockEntity extends EngineEntity implements HasFluidStorage, MenuBehavior.HasMenu {

    // Synced GUI data indices.
    public static final int DATA_ENERGY = 0; // 0..10000 fill fraction
    public static final int DATA_TEMPERATURE = 1;
    public static final int DATA_MAX_TEMP = 2;
    public static final int DATA_GENERATION = 3;
    public static final int DATA_FUEL_ID = 4;
    public static final int DATA_FUEL_AMOUNT = 5;
    public static final int DATA_FUEL_CAPACITY = 6;
    public static final int DATA_COOLANT_ID = 7;
    public static final int DATA_COOLANT_AMOUNT = 8;
    public static final int DATA_COOLANT_CAPACITY = 9;
    public static final int DATA_SHUTDOWN = 10;
    public static final int DATA_COMMITTED_FUEL = 11;
    public static final int DATA_COMMITTED_COOLING = 12;
    public static final int DATA_FUEL_BURN = 13; // committed-fuel fraction, 0..1000
    public static final int DATA_COUNT = 14;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyFraction();
                case DATA_TEMPERATURE -> (int) Math.round(fuelSim.temperature());
                case DATA_MAX_TEMP -> (int) Math.round(fuelSim.maxTemperature());
                case DATA_GENERATION -> (int) fuelSim.lastGenerationRate();
                case DATA_FUEL_ID -> fluidId(fuelTank);
                case DATA_FUEL_AMOUNT -> amountMb(fuelTank);
                case DATA_FUEL_CAPACITY -> capacityMb(fuelTank);
                case DATA_COOLANT_ID -> fluidId(coolantTank);
                case DATA_COOLANT_AMOUNT -> amountMb(coolantTank);
                case DATA_COOLANT_CAPACITY -> capacityMb(coolantTank);
                case DATA_SHUTDOWN -> fuelSim.thermallyShutDown() ? 1 : 0;
                case DATA_COMMITTED_FUEL -> (int) Math.min(Integer.MAX_VALUE, fuelSim.committedFuelEnergy());
                case DATA_COMMITTED_COOLING -> (int) Math.round(fuelSim.committedCoolingCapacity());
                case DATA_FUEL_BURN -> (int) Math.round(fuelSim.committedFuelFraction() * 1000);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side data source only; the client uses a SimpleContainerData populated by sync.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private EngineEnergyOutputComponent energy;
    private FluidStoreComponent fuelTank;
    private FluidStoreComponent coolantTank;
    private FuelEngineComponent fuelSim;
    private IFluidStorage fluidView;

    public FuelEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.FUEL_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuelEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    @Override
    protected void configure(EngineBuilder engine) {
        FuelEngineProfile profile = FuelEngineProfile.of(
                LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MIN_OUTPUT),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MAX_OUTPUT),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MAX_TEMPERATURE),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_WASTE_HEAT_PER_RF));

        energy = engine.energyOutput("energy")
                .capacity(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_BUFFER_CAPACITY))
                .build();
        fuelTank = engine.fluids("fuelTank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_TANK_CAPACITY)))
                .build();
        coolantTank = engine.fluids("coolantTank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsPower.CONFIG.COOLANT_TANK_CAPACITY)))
                .build();
        engine.add(new FluidSyncComponent(fuelTank, coolantTank));
        fuelSim = engine.add(new FuelEngineComponent(
                "fuelSim", energy, fuelTank, coolantTank,
                FuelEngineFuels::lookup, FuelEngineCoolants::lookup, this::isPowered, profile, this::setChanged));
        engine.pistonCycle("cycle")
                .energy(energy)
                .overheated(fuelSim::isOverheated)
                .powered(this::isPowered)
                .pistonSpeed(this::pistonSpeed)
                .outputPower(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MAX_OUTPUT))
                .outputFace(() -> FuelEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(true)
                .build();

        fluidView = new FuelEngineFluidView(fuelTank, coolantTank);
    }

    /** Piston speed scales with the current generation rate (harder work → faster piston). */
    private double pistonSpeed() {
        long gen = fuelSim.lastGenerationRate();
        if (gen <= 0) {
            return 0.0;
        }
        long min = LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MIN_OUTPUT);
        long max = LogisticsConfigHost.get(LogisticsPower.CONFIG.FUEL_MAX_OUTPUT);
        double t = max > min ? Math.clamp((double) (gen - min) / (max - min), 0.0, 1.0) : 1.0;
        return 0.02 + t * 0.06;
    }

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return fluidView;
    }

    // ==================== GUI/HUD accessors ====================

    public FluidStoreComponent fuelTank() {
        return fuelTank;
    }

    public FluidStoreComponent coolantTank() {
        return coolantTank;
    }

    public FuelEngineComponent simulation() {
        return fuelSim;
    }

    public long getEnergyStored() {
        return energy.getAmount();
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.fuel_engine");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                ContainerLevelAccess accessor = level == null
                        ? ContainerLevelAccess.NULL
                        : ContainerLevelAccess.create(level, getBlockPos());
                return new FuelEngineScreenHandler(syncId, playerInventory, containerData, accessor);
            }
        };
    }

    private int energyFraction() {
        long capacity = energy.getCapacity();
        return capacity <= 0 ? 0 : (int) Math.min(10_000L, energy.getAmount() * 10_000L / capacity);
    }

    private static int fluidId(FluidStoreComponent store) {
        return store.tank().isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(store.tank().getFluidKey().getFluid());
    }

    private static int amountMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getAmount()), Integer.MAX_VALUE);
    }

    private static int capacityMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getCapacity()), Integer.MAX_VALUE);
    }

    /** Insert-by-type view: fuels route to the fuel tank, water to the coolant tank; nothing is drainable. */
    private record FuelEngineFluidView(FluidStoreComponent fuelTank, FluidStoreComponent coolantTank)
            implements IFluidStorage {
        @Override
        public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
            Fluid f = fluid.getFluid();
            if (FuelEngineFuels.isFuel(f)) {
                return fuelTank.tank().insert(fluid, maxAmount, simulate);
            }
            if (FuelEngineCoolants.isCoolant(f)) {
                return coolantTank.tank().insert(fluid, maxAmount, simulate);
            }
            return 0;
        }

        @Override
        public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
            return 0; // inputs are not pipe-drainable
        }

        @Override
        public Iterable<IFluidView> contents() {
            List<IFluidView> views = new ArrayList<>();
            fuelTank.tank().contents().forEach(views::add);
            coolantTank.tank().contents().forEach(views::add);
            return views;
        }
    }
}
