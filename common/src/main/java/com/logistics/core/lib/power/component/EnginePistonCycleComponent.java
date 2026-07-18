package com.logistics.core.lib.power.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.power.CyclePhase;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.EngineCyclePlanner;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Advances the two-stroke piston cycle and pushes energy out on the compression stroke (or
 * continuously). Skips entirely while the engine is overheated, mirroring the classic engine tick.
 */
public final class EnginePistonCycleComponent implements MachineComponent, EngineComponent.PistonState {

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final EngineHeatComponent heat;
    private final BooleanSupplier powered;
    private final LongSupplier outputPower;
    private final Supplier<Direction> outputFace;
    private final DoubleSupplier pistonSpeed;
    private final boolean sendsEnergyContinuously;
    private final Runnable onChanged;

    private float progress;
    private CyclePhase cyclePhase = CyclePhase.IDLE;

    public EnginePistonCycleComponent(
            String id,
            EngineEnergyOutputComponent energy,
            EngineHeatComponent heat,
            BooleanSupplier powered,
            LongSupplier outputPower,
            Supplier<Direction> outputFace,
            DoubleSupplier pistonSpeed,
            boolean sendsEnergyContinuously,
            Runnable onChanged) {
        this.id = id;
        this.energy = energy;
        this.heat = heat;
        this.powered = powered;
        this.outputPower = outputPower;
        this.outputFace = outputFace;
        this.pistonSpeed = pistonSpeed;
        this.sendsEnergyContinuously = sendsEnergyContinuously;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        if (heat.isOverheated()) {
            return;
        }

        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                cyclePhase, progress, powered.getAsBoolean(), (float) pistonSpeed.getAsDouble(), sendsEnergyContinuously);
        cyclePhase = result.phase();
        progress = result.progress();

        if (result.shouldSendEnergy() && powered.getAsBoolean()) {
            energy.push(ctx.level(), ctx.pos(), outputFace.get(), outputPower.getAsLong());
        }
        onChanged.run();
    }

    @Override
    public float pistonSpeed() {
        return (float) pistonSpeed.getAsDouble();
    }

    /** Whether the piston is on its compression stroke (read by the heat component). */
    public boolean isCompression() {
        return cyclePhase == CyclePhase.COMPRESSION;
    }

    /** The engine's current output power in RF/t. */
    public long outputPower() {
        return outputPower.getAsLong();
    }

    public float progress() {
        return progress;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("CycleProgress", progress);
        tag.putInt("CyclePhase", cyclePhase.ordinal());
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        readState(root);
    }

    private void readState(CompoundTag tag) {
        progress = NbtCompat.getFloat(tag, "CycleProgress", 0f);
        cyclePhase = CyclePhase.fromOrdinal(NbtCompat.getInt(tag, "CyclePhase", 0));
    }
}
