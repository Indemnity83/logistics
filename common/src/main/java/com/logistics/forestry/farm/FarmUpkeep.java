package com.logistics.forestry.farm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The RF/t a farm type pays for each Plot it maintains, Empty Plots included, one config key each
 * in {@code forestry/farm.json}. Fractions build up and are paid in whole RF.
 */
public enum FarmUpkeep {
    CROP_FARM("rf_upkeep_crop_farm", 0.25, "RF per tick for each Plot the Crop Farm maintains"),
    ARBORETUM("rf_upkeep_arboretum", 1.5, "RF per tick for each Plot the Arboretum maintains"),
    NETHER_WART_FARM("rf_upkeep_nether_wart_farm", 0.25, "RF per tick for each Plot the Nether Wart Farm maintains");

    private final String configName;
    private final double defaultRfPerTick;
    private final String description;

    FarmUpkeep(String configName, double defaultRfPerTick, String description) {
        this.configName = configName;
        this.defaultRfPerTick = defaultRfPerTick;
        this.description = description;
    }

    /** The config key's name in {@code forestry/farm.json}. */
    public String configName() {
        return configName;
    }

    public double defaultRfPerTick() {
        return defaultRfPerTick;
    }

    public String description() {
        return description;
    }

    /** Every rate at its shipped default. */
    public static Map<FarmUpkeep, Double> defaults() {
        Map<FarmUpkeep, Double> rates = new EnumMap<>(FarmUpkeep.class);
        for (FarmUpkeep upkeep : values()) {
            rates.put(upkeep, upkeep.defaultRfPerTick);
        }
        return Collections.unmodifiableMap(rates);
    }
}
