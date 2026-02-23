package com.logistics.core.fabricator;

/**
 * Constants for the Kiln's energy-based PID heat control system.
 * <p>
 * The kiln uses an internal energy buffer that maps to temperature via:
 * T = TMAX * (1 - exp(-E / C))
 * <p>
 * A PID controller modulates fuel consumption to maintain the operating setpoint.
 */
public final class KilnConstants {
    private KilnConstants() {
    }

    // ========== Temperature & Energy Mapping ==========

    /**
     * Maximum theoretical temperature (°C).
     * Asymptotic limit of the exponential temperature curve.
     */
    public static final double TMAX = 2000.0;

    /**
     * Target operating temperature (°C) for the PID controller.
     * The kiln will attempt to maintain this temperature during operation.
     */
    public static final double OPERATING_SETPOINT = 1500.0;

    /**
     * Minimum temperature (°C) required to melt glass.
     * Below this temperature, melting progress will pause.
     */
    public static final double GLASS_MELT_MIN_TEMP = 1000.0;

    /**
     * Thermal mass constant (C) in the exponential temperature mapping.
     * Higher values = slower warmup. Adjust to tune warmup time.
     * Target: ~60 seconds from cold to 1500°C at full throttle.
     */
    public static final double THERMAL_MASS_C = 74000.0;

    // ========== Fuel & Energy System ==========

    /**
     * Energy added per fuel tick consumed.
     * Base unit for energy-to-fuel conversion.
     */
    public static final double ENERGY_PER_FUEL_TICK = 1.0;

    /**
     * Time constant (tau) for exponential energy decay.
     * Energy loss per tick = E / TAU_TICKS
     * Target: ~5 minutes to cool from 1500°C to 500°C.
     */
    public static final double ENERGY_LOSS_TAU_TICKS = 20000.0;

    /**
     * Base burn rate multiplier.
     * Actual burn rate = BASE_BURN_RATE * (throttle * fuelBurnCap)
     */
    public static final double BASE_BURN_RATE = 1.0;

    // ========== Fuel Burn Cap System ==========
    // Burn caps limit how fast different fuels can be consumed based on vanilla burn time.
    // Formula: cap = REFERENCE_RATE * (burnTime / REFERENCE_TIME) ^ EXPONENT

    /**
     * Reference fuel burn time (ticks) for burn cap calculation.
     * Coal = 1600 ticks in vanilla.
     */
    public static final double BURN_CAP_REFERENCE_ITEM_TIME = 1600.0;

    /**
     * Burn cap (ticks/tick) for the reference fuel at full throttle.
     * Coal can burn at 80 ticks/tick = 1 second of fuel per game tick.
     */
    public static final double BURN_CAP_REFERENCE_RATE = 80.0;

    /**
     * Exponent for burn cap scaling.
     * 0.5 = square root scaling (diminishing returns for longer-burning fuels).
     */
    public static final double BURN_CAP_EXPONENT = 0.5;

    /**
     * Minimum burn cap (ticks/tick).
     * Even very short-burning items (sticks, saplings) can burn at this rate.
     */
    public static final double BURN_CAP_MIN = 5.0;

    /**
     * Maximum burn cap (ticks/tick).
     * Caps extremely long-burning fuels (lava buckets) to prevent excessive burn rates.
     */
    public static final double BURN_CAP_MAX = 320.0;

    // ========== PID Controller Tuning ==========

    /**
     * Proportional gain (Kp).
     * Response to current error. Start conservative, increase until oscillation, back off 50%.
     */
    public static final double PID_KP = 0.01;

    /**
     * Integral gain (Ki).
     * Eliminates steady-state error. Add slowly after Kp is tuned.
     */
    public static final double PID_KI = 0.0001;

    /**
     * Derivative gain (Kd).
     * Dampens oscillations. Optional, add if system overshoots.
     */
    public static final double PID_KD = 0.02;

    /**
     * Minimum throttle during startup kick phase.
     * Ensures aggressive warmup from cold start.
     */
    public static final double MIN_THROTTLE_KICK = 0.6;

    /**
     * Exponent for startup kick falloff.
     * Higher values = sharper transition from kick to PID control.
     */
    public static final double KICK_EXPONENT = 2.0;

    // ========== Recipe Work Costs ==========

    /**
     * Energy cost per tick while melting glass.
     * Deducted from energy buffer during active melting.
     * Tune to make melting feel like it impacts temperature.
     */
    public static final double MELT_ENERGY_COST_PER_TICK = 100.0;

    /**
     * Scaling factor for recipe heat costs.
     * Old system: heat cost compared directly to heat (0-2000).
     * New system: heat cost represents energy, scale by THERMAL_MASS_C / OLD_MAX_HEAT.
     * This constant documents the expected scaling (~37x).
     */
    public static final double RECIPE_HEAT_COST_SCALE = THERMAL_MASS_C / 2000.0; // ~37
}
