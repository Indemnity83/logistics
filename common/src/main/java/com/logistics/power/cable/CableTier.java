package com.logistics.power.cable;

public enum CableTier {
    COPPER("copper_cable", "Copper Cable", 30),
    GOLD("gold_cable", "Gold Cable", 60),
    ENDER("ender_cable", "Ender Cable", 120);

    private static final String BASE_MODEL_PREFIX = "cable_";

    private final String id;
    private final String displayName;
    private final long transferRate;

    CableTier(String id, String displayName, long transferRate) {
        this.id = id;
        this.displayName = displayName;
        this.transferRate = transferRate;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public long transferRate() {
        return transferRate;
    }

    public String modelName(String baseModelName) {
        if (!baseModelName.startsWith(BASE_MODEL_PREFIX)) {
            throw new IllegalArgumentException("Cable model must start with " + BASE_MODEL_PREFIX + ": " + baseModelName);
        }
        return id + "_" + baseModelName.substring(BASE_MODEL_PREFIX.length());
    }
}
