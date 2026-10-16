package com.logistics.forestry.farm;

/**
 * How much water a till costs in a given climate:
 * {@code h = clamp(max(temperature, 0.8) × (hasPrecipitation ? 1 : 1.5), 0.5, 2) × (raining ? 0.5 : 1)}.
 * Hot and dry biomes cost up to twice the base, rain halves it.
 */
public final class Hydration {

    private static final double MIN_TEMPERATURE = 0.8;
    private static final double DRY_BIOME = 1.5;
    private static final double MIN_FACTOR = 0.5;
    private static final double MAX_FACTOR = 2.0;
    private static final double RAIN = 0.5;

    private Hydration() {}

    /** The climate factor {@code h}, between 0.25 and 2. */
    public static double factor(FarmClimate climate) {
        double base = Math.max(climate.temperature(), MIN_TEMPERATURE) * (climate.hasPrecipitation() ? 1 : DRY_BIOME);
        double clamped = Math.clamp(base, MIN_FACTOR, MAX_FACTOR);
        return climate.raining() ? clamped * RAIN : clamped;
    }

    /** {@code baseMb} scaled by {@link #factor}, rounded to the nearest mB (halves up); never negative. */
    public static int waterMb(int baseMb, FarmClimate climate) {
        return (int) Math.round(Math.max(0, baseMb) * factor(climate));
    }
}
