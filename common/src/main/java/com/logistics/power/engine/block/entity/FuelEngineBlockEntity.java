package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
public class FuelEngineBlockEntity extends EngineEntity implements HasFluidStorage {

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
