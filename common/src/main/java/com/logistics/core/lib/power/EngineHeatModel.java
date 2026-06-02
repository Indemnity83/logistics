package com.logistics.core.lib.power;

final class EngineHeatModel {
    private EngineHeatModel() {}

    static double temperature(long energy, long capacity, double floor, double max) {
        return (max - floor) * energyLevel(energy, capacity) + floor;
    }

    static double energyLevel(long energy, long capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        return energy / (double) capacity;
    }

    static double heatLevel(double temperature, double max) {
        if (max <= 0) {
            return 0.0;
        }
        return temperature / max;
    }

    static AbstractEngineBlockEntity.HeatStage stage(double temperature, double max, boolean canOverheat, boolean compression) {
        double ratio = heatLevel(temperature, max);

        if (ratio < 0.25) return AbstractEngineBlockEntity.HeatStage.COLD;
        if (ratio < 0.50) return AbstractEngineBlockEntity.HeatStage.COOL;
        if (ratio < 0.75) return AbstractEngineBlockEntity.HeatStage.WARM;
        if (ratio >= 1.0 && canOverheat) return AbstractEngineBlockEntity.HeatStage.OVERHEAT;

        return !canOverheat && compression ? AbstractEngineBlockEntity.HeatStage.WARM : AbstractEngineBlockEntity.HeatStage.HOT;
    }

    static float pistonSpeed(double temperature, double max, boolean canOverheat) {
        double ratio = heatLevel(temperature, max);
        if (ratio < 0.25) return 0.01f;
        if (ratio < 0.50) return 0.02f;
        if (ratio < 0.75) return 0.04f;
        if (ratio < 1.0 || !canOverheat) return 0.08f;
        return 0.0f;
    }
}
