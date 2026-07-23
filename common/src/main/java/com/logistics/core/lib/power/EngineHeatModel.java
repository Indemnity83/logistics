package com.logistics.core.lib.power;

public final class EngineHeatModel {
    private EngineHeatModel() {}

    public static double temperature(long energy, long capacity, double floor, double max) {
        return (max - floor) * energyLevel(energy, capacity) + floor;
    }

    public static double energyLevel(long energy, long capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        return energy / (double) capacity;
    }

    public static double heatLevel(double temperature, double max) {
        if (max <= 0) {
            return 0.0;
        }
        return temperature / max;
    }

    public static HeatStage stage(double temperature, double max, boolean canOverheat, boolean compression) {
        double ratio = heatLevel(temperature, max);

        if (ratio < 0.25) return HeatStage.COLD;
        if (ratio < 0.50) return HeatStage.COOL;
        if (ratio < 0.75) return HeatStage.WARM;
        if (ratio >= 1.0 && canOverheat) return HeatStage.OVERHEAT;

        return !canOverheat && compression ? HeatStage.WARM : HeatStage.HOT;
    }

    public static float pistonSpeed(double temperature, double max, boolean canOverheat) {
        double ratio = heatLevel(temperature, max);
        if (ratio < 0.25) return 0.01f;
        if (ratio < 0.50) return 0.02f;
        if (ratio < 0.75) return 0.04f;
        if (ratio < 1.0 || !canOverheat) return 0.08f;
        return 0.0f;
    }
}
