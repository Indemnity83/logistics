package com.logistics.power.engine.block.entity;

import com.logistics.power.engine.PIDController;

final class StirlingGenerationPlanner {
    private final PIDController pidController;
    private final double targetTemperature;
    private final double defaultGeneration;

    private double currentGeneration;
    private double generationCarry;

    StirlingGenerationPlanner(
            double kp,
            double ki,
            double kd,
            double targetTemperature,
            double defaultGeneration) {
        this.pidController = new PIDController(kp, ki, kd);
        this.targetTemperature = targetTemperature;
        this.defaultGeneration = defaultGeneration;
        currentGeneration = defaultGeneration;
    }

    long generate(
            double temperature,
            long storedEnergy,
            long capacity,
            double minGeneration,
            double maxGeneration) {
        currentGeneration = pidController.compute(targetTemperature, temperature, minGeneration, maxGeneration);
        generationCarry += currentGeneration;

        long whole = (long) Math.floor(generationCarry);
        if (whole <= 0) {
            return 0;
        }

        long space = capacity - storedEnergy;
        long toAdd = Math.max(0, Math.min(whole, space));

        if (toAdd > 0) {
            generationCarry -= toAdd;
        }

        if (space <= 0) {
            generationCarry = 0.0;
        }

        return toAdd;
    }

    long outputPower(
            double temperature,
            double temperatureFloor,
            double minGeneration,
            double maxGeneration) {
        double ratio = Math.min(1.0, (temperature - temperatureFloor) / (targetTemperature - temperatureFloor));
        double output = minGeneration + ratio * (maxGeneration - minGeneration);
        return Math.round(output);
    }

    void reset() {
        pidController.reset();
        currentGeneration = defaultGeneration;
        generationCarry = 0.0;
    }

    double currentGeneration() {
        return currentGeneration;
    }

    double generationCarry() {
        return generationCarry;
    }

    double pidIntegral() {
        return pidController.getIntegral();
    }

    void restore(double currentGeneration, double generationCarry, double pidIntegral) {
        this.currentGeneration = currentGeneration;
        this.generationCarry = generationCarry;
        pidController.setIntegral(pidIntegral);
    }
}
