package com.logistics.core.lib.heat;

import com.logistics.core.lib.storage.NbtCompat;
import java.util.function.DoubleSupplier;
import net.minecraft.nbt.CompoundTag;

/**
 * Encapsulates heat/temperature management for machines and engines.
 * Provides temperature computation, heat level calculation, stage determination, and NBT persistence.
 *
 * <p>Temperature is derived from a configurable calculator (default: constant at floor).
 * Heat level is the ratio of current temperature to max temperature (0.0 to 1.0+).
 * Heat stage is computed from heat level thresholds.
 *
 * <p>Usage:
 * <pre>{@code
 * HeatComponent heat = new HeatComponent(10000, 20, this::setChanged);
 * heat.setTemperatureCalculator(() -> operationState ? 5000 : 20);
 * heat.computeTemperature();
 * HeatStage stage = heat.computeStage(true); // true = machine can overheat
 * }</pre>
 */
public class HeatComponent {
    private final double maxTemperature;
    private final double temperatureFloor;
    private final Runnable onChanged;

    private double temperature;
    private DoubleSupplier temperatureCalculator;

    /**
     * Creates a heat component with the specified parameters.
     *
     * @param maxTemperature maximum temperature (100% heat level)
     * @param temperatureFloor minimum temperature (0% heat level)
     * @param onChanged callback when temperature changes
     */
    public HeatComponent(double maxTemperature, double temperatureFloor, Runnable onChanged) {
        this.maxTemperature = maxTemperature;
        this.temperatureFloor = temperatureFloor;
        this.onChanged = onChanged;
        this.temperature = temperatureFloor;
        this.temperatureCalculator = () -> temperatureFloor; // Default: constant at floor
    }

    /**
     * Sets a custom temperature calculation function.
     * Called by {@link #computeTemperature()} to derive current temperature.
     *
     * @param calculator function that returns the current temperature
     */
    public void setTemperatureCalculator(DoubleSupplier calculator) {
        this.temperatureCalculator = calculator;
    }

    /**
     * Computes the current temperature using the configured calculator.
     * Should be called each tick to update temperature state.
     */
    public void computeTemperature() {
        temperature = temperatureCalculator.getAsDouble();
        onChanged.run();
    }

    /**
     * Gets the heat level as a ratio from 0.0 (floor) to 1.0+ (max and beyond).
     *
     * @return heat level ratio
     */
    public double getHeatLevel() {
        if (maxTemperature <= 0) {
            return 0.0;
        }
        return temperature / maxTemperature;
    }

    /**
     * Computes the current heat stage based on heat level thresholds.
     *
     * @param canOverheat whether this machine can enter OVERHEAT stage
     * @return the current heat stage
     */
    public HeatStage computeStage(boolean canOverheat) {
        double heatLevel = getHeatLevel();

        if (heatLevel < 0.25) return HeatStage.COLD;
        if (heatLevel < 0.50) return HeatStage.COOL;
        if (heatLevel < 0.75) return HeatStage.WARM;
        if (heatLevel >= 1.0 && canOverheat) return HeatStage.OVERHEAT;

        return HeatStage.HOT;
    }

    /**
     * Applies temperature decay when idle.
     * Reduces temperature by decayRate per tick, clamped to floor.
     *
     * @param decayRate temperature reduction per tick
     */
    public void applyDecay(double decayRate) {
        if (temperature > temperatureFloor) {
            temperature = Math.max(temperature - decayRate, temperatureFloor);
            onChanged.run();
        }
    }

    /**
     * Resets temperature to floor.
     * Useful when resetting from overheat or shutting down.
     */
    public void resetToFloor() {
        temperature = temperatureFloor;
        onChanged.run();
    }

    // Getters

    public double getTemperature() {
        return temperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public double getTemperatureFloor() {
        return temperatureFloor;
    }

    // NBT Persistence

    /**
     * Writes temperature data to NBT.
     *
     * @param tag the NBT tag to write to
     */
    public void writeNbt(CompoundTag tag) {
        tag.putDouble("Temperature", temperature);
    }

    /**
     * Reads temperature data from NBT.
     *
     * @param tag the NBT tag to read from
     */
    public void readNbt(CompoundTag tag) {
        temperature = NbtCompat.getDouble(tag, "Temperature", temperatureFloor);
    }
}
