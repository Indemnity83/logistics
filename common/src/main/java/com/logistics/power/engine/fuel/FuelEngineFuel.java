package com.logistics.power.engine.fuel;

/**
 * Combustion properties of a liquid fuel the Fuel Engine can burn: the RF released per bucket and the
 * base heat generated each tick while it burns.
 */
public record FuelEngineFuel(long energyPerBucket, double heatPerTick) {

    /** RF committed when a {@code batchMb}-sized batch is consumed. */
    public long energyPerBatch(int batchMb) {
        return energyPerBucket * batchMb / 1000;
    }
}
