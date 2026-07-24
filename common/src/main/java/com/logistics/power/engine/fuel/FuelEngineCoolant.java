package com.logistics.power.engine.fuel;

/**
 * Cooling properties of a Fuel Engine coolant: the heat units removable per mB (loaded as a reserve
 * when a batch is committed) and the maximum heat it can remove per tick.
 */
public record FuelEngineCoolant(double coolingCapacityPerMb, double maximumCoolingPerTick) {

    /** Cooling capacity loaded when a {@code batchMb}-sized batch is committed. */
    public double capacityPerBatch(int batchMb) {
        return coolingCapacityPerMb * batchMb;
    }
}
