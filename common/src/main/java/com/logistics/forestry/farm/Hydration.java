package com.logistics.forestry.farm;

/**
 * How much irrigating farmland costs in a given climate:
 * {@code h = clamp(max(temperature, 0.8) × (hasPrecipitation ? 1 : 1.5), 0.5, 2)}.
 * Hot and dry biomes cost up to twice the base. Rain is not priced here: it wets farmland for free.
 */
public final class Hydration {

    private static final double MIN_TEMPERATURE = 0.8;
    private static final double DRY_BIOME = 1.5;
    private static final double MIN_FACTOR = 0.5;
    private static final double MAX_FACTOR = 2.0;

    private Hydration() {}

    /** The climate factor {@code h}, between 0.8 and 2. */
    public static double factor(FarmClimate climate) {
        double base = Math.max(climate.temperature(), MIN_TEMPERATURE) * (climate.hasPrecipitation() ? 1 : DRY_BIOME);
        return Math.clamp(base, MIN_FACTOR, MAX_FACTOR);
    }

    /** {@code baseMb} scaled by {@link #factor}, rounded to the nearest mB (halves up); never negative. */
    public static int waterMb(int baseMb, FarmClimate climate) {
        return (int) Math.round(Math.max(0, baseMb) * factor(climate));
    }
}
