package com.logistics.power.cable;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;

public enum CableTier {
    BASIC("basic_cable", "Basic Cable"),
    CONDUCTIVE("conductive_cable", "Conductive Cable"),
    RESONANT("resonant_cable", "Resonant Cable"),
    DEEP("deep_cable", "Deep Cable");

    private static final String BASE_MODEL_PREFIX = "cable_";

    private final String id;
    private final String displayName;

    CableTier(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** RF/tick throughput limit, sourced from the {@code power/cables} config. */
    public long transferRate() {
        return switch (this) {
            case BASIC -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_BASIC_TRANSFER);
            case CONDUCTIVE -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_CONDUCTIVE_TRANSFER);
            case RESONANT -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_RESONANT_TRANSFER);
            case DEEP -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_DEEP_TRANSFER);
        };
    }

    public String modelName(String baseModelName) {
        if (!baseModelName.startsWith(BASE_MODEL_PREFIX)) {
            throw new IllegalArgumentException("Cable model must start with " + BASE_MODEL_PREFIX + ": " + baseModelName);
        }
        return id + "_" + baseModelName.substring(BASE_MODEL_PREFIX.length());
    }
}
