package com.logistics.power.block.entity;

import java.util.Arrays;

final class CreativeSinkDrainState {
    static final long[] DEFAULT_DRAIN_RATES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100, Long.MAX_VALUE};
    private static final int DEFAULT_DRAIN_RATE_INDEX = 4;

    private final long[] drainRates;
    private int drainRateIndex = DEFAULT_DRAIN_RATE_INDEX;
    private long energyLastTick;
    private long energyThisTick;
    private long totalEnergyReceived;

    CreativeSinkDrainState() {
        this(DEFAULT_DRAIN_RATES);
    }

    CreativeSinkDrainState(long[] drainRates) {
        if (drainRates.length == 0) {
            throw new IllegalArgumentException("drainRates must not be empty");
        }

        this.drainRates = Arrays.copyOf(drainRates, drainRates.length);
        if (this.drainRates.length <= DEFAULT_DRAIN_RATE_INDEX) {
            drainRateIndex = 0;
        }
    }

    long insert(long maxAmount, boolean simulate) {
        long toAccept = Math.min(maxAmount, networkDemandPerTick());
        if (toAccept > 0 && !simulate) {
            energyThisTick += toAccept;
        }
        return toAccept;
    }

    void tick() {
        energyLastTick = energyThisTick;

        if (energyThisTick > 0 && Long.MAX_VALUE - totalEnergyReceived <= energyThisTick) {
            totalEnergyReceived = Long.MAX_VALUE;
        } else {
            totalEnergyReceived += energyThisTick;
        }

        energyThisTick = 0;
    }

    long drainRate() {
        return drainRates[drainRateIndex];
    }

    long networkDemandPerTick() {
        long drainRate = drainRate();
        if (drainRate == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0, drainRate - energyThisTick);
    }

    long cycle() {
        drainRateIndex = (drainRateIndex + 1) % drainRates.length;
        return drainRate();
    }

    void setUnlimited() {
        drainRateIndex = drainRates.length - 1;
    }

    int index() {
        return drainRateIndex;
    }

    void restore(int index) {
        if (index < 0 || index >= drainRates.length) {
            drainRateIndex = Math.min(DEFAULT_DRAIN_RATE_INDEX, drainRates.length - 1);
            return;
        }

        drainRateIndex = index;
    }

    long energyLastTick() {
        return energyLastTick;
    }

    long energyThisTick() {
        return energyThisTick;
    }

    long totalEnergyReceived() {
        return totalEnergyReceived;
    }
}
