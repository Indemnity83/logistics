package com.logistics.power.engine.steam;

import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

/**
 * The Steam Engine's stored-pressure reserve — an abstract stored-steam / pressure-energy quantity
 * (not steam mB). The boiler-half adds charge; the turbine-half draws it; a cold firebox leaks it.
 *
 * <p>This is the seam a future standalone boiler/turbine split would lift out: a boiler block would
 * {@link #add} charge to a shared vessel and a turbine block would {@link #draw} from it. A future
 * steam-fluid network may use different units and conversion ratios.
 */
public final class PressureVessel {

    private double pressure;

    public double pressure() {
        return pressure;
    }

    /** Add produced steam charge (no upper clamp here — the caller clamps to the safety maximum). */
    public void add(double amount) {
        if (amount > 0) {
            pressure += amount;
        }
    }

    /** Draw down for work; returns the amount actually drawn (never more than is stored). */
    public double draw(double amount) {
        double drawn = Math.min(Math.max(0, amount), pressure);
        pressure -= drawn;
        return drawn;
    }

    /** Passive loss from a cold boiler; never below zero. */
    public void leak(double amount) {
        pressure = Math.max(0, pressure - Math.max(0, amount));
    }

    /** Clamp into {@code [0, max]} (safety ceiling). */
    public void clampTo(double max) {
        pressure = Math.clamp(pressure, 0.0, max);
    }

    public double fraction(double max) {
        return max <= 0 ? 0 : Math.clamp(pressure / max, 0.0, 1.0);
    }

    /** Direct set, for load and tests. */
    public void set(double value) {
        pressure = Math.max(0, value);
    }

    public void save(CompoundTag tag, String key) {
        tag.putDouble(key, pressure);
    }

    public void load(CompoundTag tag, String key) {
        pressure = Math.max(0, NbtCompat.getDouble(tag, key, 0));
    }
}
