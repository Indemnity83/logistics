package com.logistics.power.engine.fuel;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.EngineHeatModel;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine simulation: a reserve-burn engine that decouples fuel, committed fuel energy,
 * generated RF, temperature, coolant, and committed cooling capacity. It burns a committed 100 mB fuel
 * batch as an RF reserve, generates 10–40 RF/t throttled by buffer space, turns un-bufferable RF into
 * extra heat, and cools with a committed water-capacity reserve. Heat is the only automatic shutdown;
 * a wrench resets it.
 *
 * <p>Plugs into {@code EngineEntity} through {@link EngineComponent.HeatState} (independent temperature,
 * not buffer-derived) and {@link EngineComponent.RunningGate}. Fuel/coolant lookups are injected so the
 * future magmatic/reactant liquid-fuel engines can reuse this component.
 */
public final class FuelEngineComponent
        implements MachineComponent, EngineComponent.HeatState, EngineComponent.RunningGate {

    private static final double EPSILON = 1e-6;

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final FluidStoreComponent fuelStore;
    private final FluidStoreComponent coolantStore;
    private final Function<Fluid, FuelEngineFuel> fuelLookup;
    private final Function<Fluid, FuelEngineCoolant> coolantLookup;
    private final BooleanSupplier powered;
    private final FuelEngineProfile profile;
    private final Runnable onChanged;

    private double temperature;
    private boolean thermallyShutDown;
    private long committedFuelEnergy;
    @Nullable
    private Fluid committedFuelType;
    private double committedCoolingCapacity;
    @Nullable
    private Fluid committedCoolantType;
    private long lastGenerationRate;
    private HeatStage syncedStage = HeatStage.COLD;
    private boolean syncedRunning;

    public FuelEngineComponent(
            String id,
            EngineEnergyOutputComponent energy,
            FluidStoreComponent fuelStore,
            FluidStoreComponent coolantStore,
            Function<Fluid, FuelEngineFuel> fuelLookup,
            Function<Fluid, FuelEngineCoolant> coolantLookup,
            BooleanSupplier powered,
            FuelEngineProfile profile,
            Runnable onChanged) {
        this.id = id;
        this.energy = energy;
        this.fuelStore = fuelStore;
        this.coolantStore = coolantStore;
        this.fuelLookup = fuelLookup;
        this.coolantLookup = coolantLookup;
        this.powered = powered;
        this.profile = profile;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        double t0 = temperature;
        long f0 = committedFuelEnergy;
        double c0 = committedCoolingCapacity;
        boolean sd0 = thermallyShutDown;

        tickSimulation(ctx);
        syncStage(ctx);

        if (temperature != t0 || committedFuelEnergy != f0 || committedCoolingCapacity != c0 || thermallyShutDown != sd0) {
            onChanged.run();
        }

        // Push a client sync when the engine starts or stops running, so the piston renderer (which reads
        // the synced isRunning() + generation-derived piston speed) starts and stops promptly.
        boolean running = powered.getAsBoolean() && !thermallyShutDown && committedFuelEnergy > 0;
        if (running != syncedRunning) {
            syncedRunning = running;
            ctx.sync();
        }
    }

    private void tickSimulation(MachineContext ctx) {
        if (thermallyShutDown) {
            idleCool(ctx);
            return;
        }
        if (!powered.getAsBoolean()) {
            idleCool(ctx); // redstone off: pause, preserve committed reserves, keep cooling.
            return;
        }
        if (committedFuelEnergy <= 0) {
            tryCommitFuelBatch();
        }
        if (committedFuelEnergy <= 0) {
            idleCool(ctx);
            return;
        }
        burn(ctx);
    }

    private void burn(MachineContext ctx) {
        FuelEngineFuel activeFuel = fuelLookup.apply(committedFuelType);
        if (activeFuel == null) {
            // Committed fuel no longer recognized (should not happen): drop it and idle.
            discardCommittedFuel();
            idleCool(ctx);
            return;
        }

        long free = energy.getCapacity() - energy.getAmount();
        long desired = Math.clamp(free, profile.minGeneration(), profile.maxGeneration());
        long released = Math.min(desired, committedFuelEnergy);
        committedFuelEnergy -= released;
        lastGenerationRate = released;

        long accepted = Math.min(released, Math.max(0, free));
        energy.addEnergy(accepted);
        long wasted = released - accepted;
        double generatedHeat = activeFuel.heatPerTick() + wasted * profile.wasteHeatPerRejectedRf();

        if (committedFuelEnergy <= 0) {
            discardCommittedFuel(); // invariant: committedFuelEnergy > 0 ⇔ committedFuelType != null
        }

        double applied = activeCool(ctx, temperature + generatedHeat, true);
        double next = temperature + generatedHeat - applied;
        temperature = Math.clamp(next, 0.0, profile.maxTemperature());
        if (next >= profile.maxTemperature()) {
            overheat();
        }
    }

    private void overheat() {
        thermallyShutDown = true;
        discardCommittedFuel(); // discard the remaining reserve; tanks + committed cooling preserved.
    }

    private void discardCommittedFuel() {
        committedFuelEnergy = 0;
        committedFuelType = null;
    }

    // ==================== Cooling ====================

    /** Idle/paused/shut-down cooling: spend committed capacity, commit only if hot, plus free passive cooling. */
    private void idleCool(MachineContext ctx) {
        lastGenerationRate = 0;
        boolean mayCommit = temperature >= profile.passiveCoolingTarget();
        double applied = activeCool(ctx, temperature, mayCommit);
        double afterActive = temperature - applied;
        double passive = Math.min(afterActive, profile.passiveCoolingRate());
        temperature = Math.max(0.0, afterActive - passive);
    }

    /** Applies committed-reserve cooling to {@code availableHeat}; may commit fresh batches when {@code mayCommit}. */
    private double activeCool(MachineContext ctx, double availableHeat, boolean mayCommit) {
        if (availableHeat <= EPSILON) {
            return 0;
        }
        clearExhaustedReserve();
        if (committedCoolantType == null && mayCommit) {
            tryCommitCoolantBatch();
        }
        if (committedCoolantType == null) {
            return 0;
        }
        FuelEngineCoolant props = coolantLookup.apply(committedCoolantType);
        if (props == null) {
            committedCoolingCapacity = 0;
            committedCoolantType = null;
            return 0;
        }
        // Proportional (Newton's-law) cooling: shed heat in proportion to the current temperature, capped
        // by the coolant's max rate. Gives a stable warm equilibrium at generatedHeat / coolingCoefficient
        // (so a cooled engine runs visibly warm, not at 0), and climbs to overheat once coolant runs out.
        double demand =
                Math.min(availableHeat * profile.coolingCoefficient(), props.maximumCoolingPerTick());
        if (committedCoolingCapacity + EPSILON < demand && mayCommit) {
            tryCommitCoolantBatch(); // refill when insufficient, not only when empty
        }
        double applied = Math.min(demand, committedCoolingCapacity);
        committedCoolingCapacity = Math.max(0.0, committedCoolingCapacity - applied);
        clearExhaustedReserve();
        return applied;
    }

    private void clearExhaustedReserve() {
        if (committedCoolingCapacity <= EPSILON) {
            committedCoolingCapacity = 0;
            committedCoolantType = null;
        }
    }

    // ==================== Batch commits (transactional) ====================

    private void tryCommitFuelBatch() {
        Fluid fluid = fuelStore.tank().getFluidKey().getFluid();
        FuelEngineFuel props = fuelLookup.apply(fluid);
        if (props == null) {
            return;
        }
        IFluidKey key = SimpleFluidKey.of(fluid);
        long batch = FluidUnits.mb(profile.fuelBatchMb());
        if (fuelStore.tank().extract(key, batch, true) != batch) {
            return; // not a full batch present
        }
        fuelStore.tank().extract(key, batch, false);
        committedFuelEnergy = props.energyPerBatch(profile.fuelBatchMb());
        committedFuelType = fluid;
    }

    private void tryCommitCoolantBatch() {
        Fluid fluid = coolantStore.tank().getFluidKey().getFluid();
        FuelEngineCoolant props = coolantLookup.apply(fluid);
        if (props == null) {
            return;
        }
        if (committedCoolantType != null && !fluid.equals(committedCoolantType)) {
            return; // never mix coolant types into one reserve
        }
        IFluidKey key = SimpleFluidKey.of(fluid);
        long batch = FluidUnits.mb(profile.coolantBatchMb());
        if (coolantStore.tank().extract(key, batch, true) != batch) {
            return;
        }
        coolantStore.tank().extract(key, batch, false);
        committedCoolingCapacity += props.capacityPerBatch(profile.coolantBatchMb());
        committedCoolantType = fluid;
    }

    // ==================== HeatState / RunningGate ====================

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public double maxTemperature() {
        return profile.maxTemperature();
    }

    @Override
    public boolean canOverheat() {
        return true;
    }

    @Override
    public boolean isOverheated() {
        return thermallyShutDown;
    }

    @Override
    public HeatStage stage() {
        return thermallyShutDown
                ? HeatStage.OVERHEAT
                : EngineHeatModel.stage(temperature, profile.maxTemperature(), true, false);
    }

    @Override
    public boolean resetOverheat(MachineContext ctx) {
        if (!thermallyShutDown) {
            return false;
        }
        thermallyShutDown = false;
        temperature = Math.min(temperature, profile.restartTemperature());
        syncStage(ctx);
        onChanged.run();
        return true;
    }

    @Override
    public boolean isRunning(MachineContext ctx) {
        return committedFuelEnergy > 0; // host ANDs isPowered() && !isOverheated()
    }

    // ==================== GUI/HUD getters ====================

    public long committedFuelEnergy() {
        return committedFuelEnergy;
    }

    /** Remaining committed fuel as a fraction of the batch it was drawn from (1 at commit, 0 when spent). */
    public double committedFuelFraction() {
        if (committedFuelType == null || committedFuelEnergy <= 0) {
            return 0.0;
        }
        FuelEngineFuel props = fuelLookup.apply(committedFuelType);
        if (props == null) {
            return 0.0;
        }
        long full = props.energyPerBatch(profile.fuelBatchMb());
        return full <= 0 ? 0.0 : Math.min(1.0, (double) committedFuelEnergy / (double) full);
    }

    public double committedCoolingCapacity() {
        return committedCoolingCapacity;
    }

    public long lastGenerationRate() {
        return lastGenerationRate;
    }

    public boolean thermallyShutDown() {
        return thermallyShutDown;
    }

    private void syncStage(MachineContext ctx) {
        HeatStage stage = stage();
        if (stage == syncedStage) {
            return;
        }
        syncedStage = stage;
        BlockState state = ctx.blockState();
        if (state.hasProperty(HeatStage.STAGE)) {
            ctx.setBlockState(state.setValue(HeatStage.STAGE, stage), Block.UPDATE_ALL);
        }
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("Temperature", temperature);
        tag.putBoolean("ThermallyShutDown", thermallyShutDown);
        tag.putLong("CommittedFuelEnergy", committedFuelEnergy);
        if (committedFuelType != null) {
            tag.putString("CommittedFuelType", BuiltInRegistries.FLUID.getKey(committedFuelType).toString());
        }
        tag.putDouble("CommittedCoolingCapacity", committedCoolingCapacity);
        if (committedCoolantType != null) {
            tag.putString("CommittedCoolantType", BuiltInRegistries.FLUID.getKey(committedCoolantType).toString());
        }
        // Persisted so it syncs to the client, which drives piston-speed rendering (recomputed each server tick).
        tag.putLong("LastGenerationRate", lastGenerationRate);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        temperature = NbtCompat.getDouble(tag, "Temperature", 0);
        thermallyShutDown = NbtCompat.getBoolean(tag, "ThermallyShutDown", false);
        committedFuelEnergy = NbtCompat.getLong(tag, "CommittedFuelEnergy", 0);
        committedFuelType = resolveFluid(NbtCompat.getString(tag, "CommittedFuelType", ""));
        committedCoolingCapacity = NbtCompat.getDouble(tag, "CommittedCoolingCapacity", 0);
        committedCoolantType = resolveFluid(NbtCompat.getString(tag, "CommittedCoolantType", ""));
        lastGenerationRate = NbtCompat.getLong(tag, "LastGenerationRate", 0);
        // Keep the invariants after a load with possibly-stale fields.
        if (committedFuelEnergy <= 0) {
            discardCommittedFuel();
        }
        clearExhaustedReserve();
        syncedStage = stage();
    }

    @Nullable
    private static Fluid resolveFluid(String id) {
        if (id.isEmpty()) {
            return null;
        }
        ResourceId resourceId = ResourceId.tryParse(id);
        if (resourceId == null) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(resourceId.toIdentifier());
        return (fluid == null || fluid == Fluids.EMPTY) ? null : fluid;
    }
}
