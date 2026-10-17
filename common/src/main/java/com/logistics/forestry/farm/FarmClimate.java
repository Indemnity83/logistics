package com.logistics.forestry.farm;

/** The climate at a plot, as far as watering goes: the biome's base temperature and whether it gets any precipitation. */
public record FarmClimate(float temperature, boolean hasPrecipitation) {

    /** A climate whose {@link Hydration#factor} is exactly 1. */
    public static final FarmClimate NEUTRAL = new FarmClimate(1.0f, true);
}
