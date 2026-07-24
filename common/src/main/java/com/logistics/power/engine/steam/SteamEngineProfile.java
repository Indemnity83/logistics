package com.logistics.power.engine.steam;

/**
 * Centralized, tunable balance profile for a Steam Engine — the single place the simulation reads its
 * numbers from. All values are config-backed (see {@code LogisticsPower.CONFIG} group
 * {@code engines.steam}).
 *
 * <p>Two independent scales: <b>boiler heat</b> (an abstract thermal mass — the engine's primary state)
 * and <b>pressure</b> (the only usable stored energy — an abstract stored-steam quantity, not mB). Fuel
 * makes heat, heat makes steam (consuming latent heat), steam is pressure, pressure spends into RF.
 */
public record SteamEngineProfile(
        long maxOutput,
        double maxPressure,
        double operatingPressure,
        double targetPressure,
        double pressurePerRf,
        double steamPerWaterMb,
        double steamRate,
        double condensationRate,
        double maxBoilerHeat,
        double boilingHeat,
        double refuelHeat,
        double targetHeat,
        double heatPerBurnTick,
        int firingRate,
        double passiveHeatLoss,
        double latentHeat) {

    /** RF the turbine wants at a given pressure: flat max at/above operating, linear ramp to 0 below. */
    public long desiredOutput(double pressure) {
        if (pressure >= operatingPressure) {
            return maxOutput;
        }
        return (long) Math.floor(maxOutput * pressure / operatingPressure);
    }

    /**
     * Steam-quality factor: 0 at the boiling point, ramping to 1 at the steam-quality target. Below
     * boiling no steam forms; at/above target, steam is produced at the full {@link #steamRate}.
     */
    public double heatFactor(double boilerHeat) {
        double span = targetHeat - boilingHeat;
        if (span <= 0) {
            return boilerHeat >= boilingHeat ? 1.0 : 0.0;
        }
        return Math.clamp((boilerHeat - boilingHeat) / span, 0.0, 1.0);
    }
}
