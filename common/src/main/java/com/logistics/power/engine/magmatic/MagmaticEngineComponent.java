package com.logistics.power.engine.magmatic;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * The Magmatic Engine simulation: a thermal-gradient generator. It burns a committed 100 mB lava batch as
 * a fixed-duration heat source ({@code remainingBurnTicks}), heat-soaks a normalized temperature from
 * Cold → Warm → Hot while running, and generates RF <em>continuously</em> with temperature
 * ({@link MagmaticEngineProfile#outputAtTemperature}). Unused RF is discarded — the batch cools whether or
 * not the RF is consumed. It has a real, non-overheating heat state; it never overheats, needs no coolant,
 * and has no wrench reset.
 *
 * <pre>{@code
 * lava (100 mB) → remainingBurnTicks → heat-soak temperature → continuous 5..15 RF/t → RF buffer
 * }</pre>
 *
 * <p>Plugs into {@code EngineEntity} through {@link EngineComponent.HeatState} (a normalized soak, not
 * buffer-derived) and {@link EngineComponent.RunningGate}. The lava-validity {@link Predicate} is injected
 * so unit tests can drive it without the lava tag bound.
 */
public final class MagmaticEngineComponent
        implements MachineComponent, EngineComponent.HeatState, EngineComponent.RunningGate {

    // Display-only mapping of the normalized soak onto a readable temperature (no behavioral effect):
    // ambient at soak 0, up to roughly half the average temperature of lava when fully heat-soaked.
    private static final double AMBIENT_CELSIUS = 20.0;
    private static final double SOAKED_CELSIUS = 500.0;

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final FluidStoreComponent lavaStore;
    private final Predicate<Fluid> isFuel;
    private final BooleanSupplier powered;
    private final MagmaticEngineProfile profile;
    private final Runnable onChanged;

    // Persisted: temperature is the single source of truth.
    private int remainingBurnTicks;
    private double temperature;

    // Derived / transient (never persisted).
    private HeatStage stage = HeatStage.COLD; // hysteresis; seeded from temperature on load
    private long lastAttempted;
    private long lastAccepted;
    private long lastWasted;
    private HeatStage syncedStage = HeatStage.COLD;
    private boolean syncedRunning;

    public MagmaticEngineComponent(
            String id,
            EngineEnergyOutputComponent energy,
            FluidStoreComponent lavaStore,
            Predicate<Fluid> isFuel,
            BooleanSupplier powered,
            MagmaticEngineProfile profile,
            Runnable onChanged) {
        this.id = id;
        this.energy = energy;
        this.lavaStore = lavaStore;
        this.isFuel = isFuel;
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
        int b0 = remainingBurnTicks;
        double t0 = temperature;

        tickSimulation(ctx);
        boolean stageFlipped = stage != syncedStage; // capture before syncStage consumes it
        syncStage(ctx); // block-state STAGE (tint) updates only on a stage change

        // Persist every thermal tick so temperature never rolls back on save (distinct from network sync).
        if (remainingBurnTicks != b0 || temperature != t0) {
            onChanged.run();
        }

        // Runtime sync on a running flip (piston start/stop) or a stage flip (so the client's stage-based
        // piston speed matches the freshly-synced heat tint promptly).
        boolean running = isRunning();
        if (running != syncedRunning || stageFlipped) {
            syncedRunning = running;
            ctx.sync();
        }
    }

    private void tickSimulation(MachineContext ctx) {
        if (!powered.getAsBoolean()) {
            // Redstone pause: preserve the committed reserve unconsumed; still cool.
            clearOutput();
            cool();
            recalcStage();
            return;
        }
        if (!isLit()) {
            // Idle → cool this tick, then try to (re)ignite a fresh 100 mB batch.
            cool();
            recalcStage();
            if (!tryCommitLavaBatch()) {
                clearOutput();
                return; // no admission space, or no valid lava
            }
            // Committed a batch; generate this tick, matching the Fuel Engine convention (commit-then-burn).
        }
        // Lit: heat-soak FIRST, then compute this tick's continuous output from the updated temperature.
        heatSoak();
        recalcStage();
        long attempted = profile.outputAtTemperature(temperature);
        remainingBurnTicks--; // burns regardless of demand
        long free = Math.max(0, energy.getCapacity() - energy.getAmount());
        long accepted = Math.min(attempted, free);
        if (accepted > 0) {
            energy.addEnergy(accepted);
        }
        lastAttempted = attempted;
        lastAccepted = accepted;
        lastWasted = attempted - accepted; // discarded — never stored or converted to heat
    }

    /** Consume a whole 100 mB lava batch, gated by admission space; returns whether a batch was committed. */
    private boolean tryCommitLavaBatch() {
        long free = energy.getCapacity() - energy.getAmount();
        if (free < profile.maximumBatchPotentialRf()) {
            return false; // never commit lava into a backed-up buffer
        }
        Fluid fluid = lavaStore.tank().getFluidKey().getFluid();
        if (!isFuel.test(fluid)) {
            return false;
        }
        IFluidKey key = SimpleFluidKey.of(fluid);
        long batch = FluidUnits.mb(profile.batchMb());
        if (lavaStore.tank().extract(key, batch, true) != batch) {
            return false; // not a full batch present — atomic
        }
        lavaStore.tank().extract(key, batch, false);
        remainingBurnTicks = profile.batchBurnTicks();
        return true;
    }

    private void heatSoak() {
        temperature = Math.clamp(temperature + (1.0 - temperature) * profile.heatSoakRate(), 0.0, 1.0);
    }

    private void cool() {
        temperature = Math.clamp(temperature - temperature * profile.coolingRate(), 0.0, 1.0);
    }

    private void clearOutput() {
        lastAttempted = 0;
        lastAccepted = 0;
        lastWasted = 0;
    }

    /** Advance the hysteresis stage from the current temperature (Cold/Warm/Hot → COLD/COOL/WARM). */
    private void recalcStage() {
        switch (stage) {
            case COLD -> {
                if (temperature >= profile.coldToWarmThreshold()) {
                    stage = HeatStage.COOL;
                }
            }
            case COOL -> {
                if (temperature <= profile.warmToColdThreshold()) {
                    stage = HeatStage.COLD;
                } else if (temperature >= profile.warmToHotThreshold()) {
                    stage = HeatStage.WARM;
                }
            }
            case WARM -> {
                if (temperature <= profile.hotToWarmThreshold()) {
                    stage = HeatStage.COOL;
                }
            }
            default -> stage = stageFor(temperature); // HOT/OVERHEAT never occur; defensive re-seed
        }
    }

    /** Stateless stage from temperature (load seeding), using the display-band midpoints — never the transitions. */
    private HeatStage stageFor(double t) {
        if (t < profile.warmBandFloor()) {
            return HeatStage.COLD;
        }
        if (t < profile.hotBandFloor()) {
            return HeatStage.COOL;
        }
        return HeatStage.WARM;
    }

    private boolean isLit() {
        return remainingBurnTicks > 0;
    }

    /** Full running state (powered AND lit) — drives the piston and the client sync flip. */
    public boolean isRunning() {
        return powered.getAsBoolean() && isLit();
    }

    /**
     * Piston speed tied to the visible heat stage (like the Redstone Engine): slow when cold, faster as it
     * heat-soaks — COLD (blue) → COOL (green/"Warm") → WARM (yellow/"Hot"). Zero when not running.
     */
    public float pistonSpeed() {
        if (!isRunning()) {
            return 0f;
        }
        return switch (stage) {
            case COLD -> 0.02f;
            case COOL -> 0.04f;
            default -> 0.08f; // WARM — the engine's hottest visible stage
        };
    }

    // ==================== HeatState / RunningGate ====================

    @Override
    public double temperature() {
        return temperature * 100.0; // framework 0..maxTemperature scale
    }

    @Override
    public double maxTemperature() {
        return 100.0;
    }

    @Override
    public boolean canOverheat() {
        return false;
    }

    @Override
    public boolean isOverheated() {
        return false;
    }

    @Override
    public HeatStage stage() {
        return stage;
    }

    @Override
    public boolean resetOverheat(MachineContext ctx) {
        return false; // cannot overheat, nothing to reset
    }

    @Override
    public boolean isRunning(MachineContext ctx) {
        return isLit(); // host ANDs isPowered() && !isOverheated()
    }

    // ==================== GUI/HUD getters ====================

    public int remainingBurnTicks() {
        return remainingBurnTicks;
    }

    public int batchBurnTicks() {
        return profile.batchBurnTicks();
    }

    public boolean lit() {
        return isLit();
    }

    /** Boiler temperature in °C for the GUI/HUD (ambient at soak 0 → hot when fully soaked). Display only. */
    public int temperatureCelsius() {
        return (int) Math.round(AMBIENT_CELSIUS + temperature * (SOAKED_CELSIUS - AMBIENT_CELSIUS));
    }

    public long lastAttempted() {
        return lastAttempted;
    }

    public long lastAccepted() {
        return lastAccepted;
    }

    public long lastWasted() {
        return lastWasted;
    }

    // ==================== Sync / persistence ====================

    private void syncStage(MachineContext ctx) {
        if (stage == syncedStage) {
            return;
        }
        syncedStage = stage;
        BlockState state = ctx.blockState();
        if (state.hasProperty(HeatStage.STAGE)) {
            ctx.setBlockState(state.setValue(HeatStage.STAGE, stage), Block.UPDATE_ALL);
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        // Only the burn timer and temperature are authoritative. Stage is derived; accepted/wasted are
        // transient; the tank and RF buffer persist through their own components.
        tag.putInt("RemainingBurnTicks", remainingBurnTicks);
        tag.putDouble("Temperature", temperature);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        remainingBurnTicks = Math.clamp(NbtCompat.getInt(tag, "RemainingBurnTicks", 0), 0, profile.batchBurnTicks());
        temperature = Math.clamp(NbtCompat.getDouble(tag, "Temperature", 0), 0.0, 1.0);
        stage = stageFor(temperature); // re-derived, never persisted
        syncedStage = stage;
        clearOutput();
    }
}
