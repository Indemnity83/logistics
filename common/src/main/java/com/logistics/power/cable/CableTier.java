package com.logistics.power.cable;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;

public enum CableTier {
    COPPER("copper_cable", "Copper Cable"),
    GOLD("gold_cable", "Gold Cable"),
    AMETHYST("amethyst_cable", "Amethyst Cable"),
    ECHO("echo_cable", "Echo Cable");

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
            case COPPER -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_COPPER_TRANSFER);
            case GOLD -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_GOLD_TRANSFER);
            case AMETHYST -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_AMETHYST_TRANSFER);
            case ECHO -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CABLE_ECHO_TRANSFER);
        };
    }

    public String modelName(String baseModelName) {
        if (!baseModelName.startsWith(BASE_MODEL_PREFIX)) {
            throw new IllegalArgumentException("Cable model must start with " + BASE_MODEL_PREFIX + ": " + baseModelName);
        }
        return id + "_" + baseModelName.substring(BASE_MODEL_PREFIX.length());
    }
}
