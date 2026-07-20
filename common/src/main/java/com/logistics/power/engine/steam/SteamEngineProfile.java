package com.logistics.power.engine.steam;

/**
 * Centralized, tunable balance profile for a Steam Engine — the single place the pressure simulation
 * reads its numbers from. All values are config-backed (see {@code LogisticsPower.CONFIG} group
 * {@code engines.steam}). Pressure is an abstract stored-steam / pressure-energy reserve, not steam mB.
 */
public record SteamEngineProfile(
        long maxOutput,
        double maxPressure,
        double operatingPressure,
        double relightPressure,
        double targetPressure,
        double steamPerBurnTick,
        double pressurePerRf,
        double steamPerWaterMb,
        double coolingDecayPerTick,
        int stokedBurnInterval) {

    /** RF the turbine wants at a given pressure: flat max at/above operating, linear ramp to 0 below. */
    public long desiredOutput(double pressure) {
        if (pressure >= operatingPressure) {
            return maxOutput;
        }
        return (long) Math.floor(maxOutput * pressure / operatingPressure);
    }
}
