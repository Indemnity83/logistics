package com.logistics.core.lib.power.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.fuel.CoolantSource;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.lib.power.gen.Generation;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * The general fuel-burning engine model: consumes fuel to start a burn timer, and while the timer is
 * non-zero generates RF each tick and keeps the engine LIT. When the timer hits zero it asks the
 * {@link FuelSource} to ignite again; a {@link CoolantSource} (default {@link CoolantSource#NONE})
 * gates and is consumed per refuel. The {@link Generation} strategy decides how much RF each burn
 * tick produces.
 */
public final class EngineBurnComponent implements MachineComponent, EngineComponent.RunningGate {

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final EngineHeatComponent heat;
    private final BooleanSupplier powered;
    private final FuelSource fuel;
    private final Generation generation;
    private final CoolantSource coolant;
    private final EngineComponent.LitController lit;
    private final Runnable onChanged;

    private int burnTime;
    private int fuelTime;
    private boolean wasRunning;

    public EngineBurnComponent(
            String id,
            EngineEnergyOutputComponent energy,
            EngineHeatComponent heat,
            BooleanSupplier powered,
            FuelSource fuel,
            Generation generation,
            CoolantSource coolant,
            EngineComponent.LitController lit,
            Runnable onChanged) {
        this.id = id;
        this.energy = energy;
        this.heat = heat;
        this.powered = powered;
        this.fuel = fuel;
        this.generation = generation;
        this.coolant = coolant;
        this.lit = lit;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        boolean isPowered = powered.getAsBoolean();
        boolean overheated = heat.isOverheated();

        if (!isPowered || overheated) {
            burnTime = 0;
            fuelTime = 0;
        } else {
            // Mirrors fuelState.burn() || refuel(): decrement first, refuel only once burnt out.
            boolean burning;
            if (burnTime > 0) {
                burnTime--;
                burning = burnTime > 0;
            } else {
                burning = false;
            }
            if (!burning) {
                burning = tryRefuel(ctx);
            }
            if (burning) {
                long rf = generation.generate(ctx, heat.temperature(), energy.getAmount(), energy.getCapacity());
                if (rf > 0) {
                    energy.addEnergy(rf);
                }
            }
        }

        boolean running = isPowered && !overheated && burnTime > 0;
        if (wasRunning && !running) {
            generation.reset();
        }
        wasRunning = running;

        lit.setLit(ctx, burnTime > 0);
        onChanged.run();
    }

    private boolean tryRefuel(MachineContext ctx) {
        if (!coolant.available(ctx)) {
            return false;
        }
        int ticks = fuel.ignite(ctx);
        if (ticks <= 0) {
            return false;
        }
        fuelTime = ticks;
        burnTime = ticks;
        coolant.consume(ctx);
        return true;
    }

    @Override
    public boolean isRunning(MachineContext ctx) {
        return burnTime > 0;
    }

    public int burnTime() {
        return burnTime;
    }

    public int fuelTime() {
        return fuelTime;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("BurnTimeRemaining", burnTime);
        tag.putInt("TotalFuelTime", fuelTime);
        generation.save(tag, registries);
        fuel.save(tag, registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag, registries);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        readState(root, registries);
    }

    private void readState(CompoundTag tag, HolderLookup.Provider registries) {
        burnTime = Math.max(0, NbtCompat.getInt(tag, "BurnTimeRemaining", 0));
        fuelTime = Math.max(0, NbtCompat.getInt(tag, "TotalFuelTime", 0));
        generation.load(tag, registries);
        fuel.load(tag, registries);
    }
}
