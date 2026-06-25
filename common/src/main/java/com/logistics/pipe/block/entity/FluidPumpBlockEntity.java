package com.logistics.pipe.block.entity;

import com.logistics.LogisticsFluid;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid Pump: an RF-powered block that lowers an intake tube and drains connected fluid sources into
 * a buffer tank, pushing the overflow out to adjacent pipes/tanks.
 *
 * <p>Composed from machine components — an energy buffer (reports network demand), a tank, and a
 * {@link FluidPumpComponent} that owns the pumping logic. Energy and fluid are exposed on every face
 * except the bottom, which is the intake. The pump has no GUI.
 */
public class FluidPumpBlockEntity extends MachineEntity implements AcceptsLowTierEnergy {

    public enum Phase {
        DESCENDING,
        PUMPING,
        STALLED
    }

    private EnergyStorageComponent energy;
    private FluidStoreComponent fluidStore;
    private FluidPumpComponent pump;

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        LogisticsConfig.FluidPumpConfig cfg = LogisticsConfig.get().fluidPump;
        energy = machine.energy("energy")
                .capacity(cfg.energyCapacity)
                .maxInput(cfg.maxEnergyInput)
                .providesDemand()
                .build();
        fluidStore = machine.fluids("tank")
                .capacity(FluidUnits.mb(cfg.tankCapacityMb))
                .build();
        pump = machine.add(new FluidPumpComponent("pump", energy, fluidStore, getBlockPos()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity be) {
        // Server: run the component logic. Both sides: advance the tube so the descent animates
        // smoothly between the sparse state syncs.
        if (!level.isClientSide()) {
            MachineEntity.tick(level, pos, state, be);
        }
        be.pump.advanceArm(be);
    }

    @Override
    @Nullable
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        // Top and sides only; the bottom is the intake tube.
        return side == Direction.DOWN ? null : super.energyStorage(side);
    }

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return side == Direction.DOWN ? null : super.fluidStorage(side);
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(Direction from) {
        return from != Direction.DOWN;
    }

    @Override
    @Nullable
    public MenuProvider createMenuProvider() {
        // The pump has no GUI; the block never opens a menu.
        return null;
    }

    public FluidTankComponent tank() {
        return fluidStore.tank();
    }

    public Phase phase() {
        return pump.phase();
    }

    public float armY() {
        return pump.armY();
    }

    public long energyAmount() {
        return energy.amount();
    }
}
