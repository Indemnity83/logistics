package com.logistics.forestry.farm;

import com.logistics.core.machine.upgrade.OperationKey;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The world-changing actions a farm charges RF for, one config key each in
 * {@code forestry/farm.json}. RF is spent only when the action succeeds; inspecting a position is free.
 */
public enum FarmCost implements OperationKey {
    /** Harvesting one crop block. */
    HARVEST_CROP("harvest_crop", "rf_harvest_crop", 100, "RF per crop block harvested"),
    /** Felling one log of a tree. */
    HARVEST_LOG("harvest_log", "rf_harvest_log", 50, "RF per log felled"),
    /** Breaking one leaf or other canopy block of a felled tree. */
    HARVEST_LEAVES("harvest_leaves", "rf_harvest_leaves", 10, "RF per leaf or other canopy block of a felled tree"),
    /** Tilling one block, e.g. dirt into farmland. */
    TILL("till", "rf_till", 50, "RF per block tilled"),
    /** Planting one germling. */
    PLANT("plant", "rf_plant", 50, "RF per germling planted");

    private final String namespace;
    private final String configName;
    private final long defaultRf;
    private final String description;

    FarmCost(String operation, String configName, long defaultRf, String description) {
        this.namespace = "farm." + operation;
        this.configName = configName;
        this.defaultRf = defaultRf;
        this.description = description;
    }

    @Override
    public String namespace() {
        return namespace;
    }

    /** The config key's name in {@code forestry/farm.json}. */
    public String configName() {
        return configName;
    }

    public long defaultRf() {
        return defaultRf;
    }

    public String description() {
        return description;
    }

    /** Every cost at its shipped default. */
    public static Map<FarmCost, Long> defaults() {
        Map<FarmCost, Long> costs = new EnumMap<>(FarmCost.class);
        for (FarmCost cost : values()) {
            costs.put(cost, cost.defaultRf);
        }
        return Collections.unmodifiableMap(costs);
    }
}
