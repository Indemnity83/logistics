package com.logistics.power.engine.block.entity;

final class StirlingFuelState {
    private int burnTime;
    private int fuelTime;

    boolean burn() {
        if (burnTime > 0) {
            burnTime--;
        }
        return isBurning();
    }

    void ignite(int burnTicks) {
        fuelTime = Math.max(0, burnTicks);
        burnTime = fuelTime;
    }

    void extinguish() {
        burnTime = 0;
        fuelTime = 0;
    }

    void restore(int burnTime, int fuelTime) {
        this.burnTime = Math.max(0, burnTime);
        this.fuelTime = Math.max(0, fuelTime);
    }

    boolean isBurning() {
        return burnTime > 0;
    }

    int burnTime() {
        return burnTime;
    }

    int fuelTime() {
        return fuelTime;
    }
}
