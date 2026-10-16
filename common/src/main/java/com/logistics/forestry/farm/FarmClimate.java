package com.logistics.forestry.farm;

/**
 * The climate at a plot, as far as watering goes: the biome's base temperature, whether the biome
 * gets any precipitation, and whether it is raining onto the plot right now.
 */
public record FarmClimate(float temperature, boolean hasPrecipitation, boolean raining) {

    /** A climate whose {@link Hydration#factor} is exactly 1. */
    public static final FarmClimate NEUTRAL = new FarmClimate(1.0f, true, false);
}
