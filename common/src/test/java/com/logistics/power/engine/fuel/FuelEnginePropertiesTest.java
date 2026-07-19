package com.logistics.power.engine.fuel;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsCore;
import org.junit.jupiter.api.Test;

/**
 * Property-math and fuel-table content for the Fuel Engine. Positive {@code Fluid}-instance lookups
 * (which need the mod's custom fluids + water tag registered) are covered by the game test, not here.
 */
class FuelEnginePropertiesTest {

    @Test
    void fuelEnergyPerBatch_100mb() {
        assertThat(new FuelEngineFuel(40_000L, 3.0).energyPerBatch(100)).isEqualTo(4_000L);
        assertThat(new FuelEngineFuel(80_000L, 1.5).energyPerBatch(100)).isEqualTo(8_000L);
        assertThat(new FuelEngineFuel(150_000L, 2.25).energyPerBatch(100)).isEqualTo(15_000L);
    }

    @Test
    void runtimeAt40RfPerTick() {
        assertThat(new FuelEngineFuel(40_000L, 3.0).energyPerBatch(100) / 40).isEqualTo(100L);
        assertThat(new FuelEngineFuel(80_000L, 1.5).energyPerBatch(100) / 40).isEqualTo(200L);
        assertThat(new FuelEngineFuel(150_000L, 2.25).energyPerBatch(100) / 40).isEqualTo(375L);
    }

    @Test
    void coolantCapacityPerBatch() {
        assertThat(new FuelEngineCoolant(4.0, 6.0).capacityPerBatch(100)).isEqualTo(400.0);
    }

    @Test
    void fuelTableByRegistryId() {
        assertThat(FuelEngineFuels.byId(LogisticsCore.resource("crude_oil").toIdentifier()))
                .isEqualTo(new FuelEngineFuel(40_000L, 3.0));
        assertThat(FuelEngineFuels.byId(LogisticsCore.resource("bio_fuel").toIdentifier()))
                .isEqualTo(new FuelEngineFuel(80_000L, 1.5));
        assertThat(FuelEngineFuels.byId(LogisticsCore.resource("fuel_oil").toIdentifier()))
                .isEqualTo(new FuelEngineFuel(150_000L, 2.25));
        assertThat(FuelEngineFuels.byId(LogisticsCore.resource("liquid_redstone").toIdentifier())).isNull();
        assertThat(FuelEngineFuels.byId(null)).isNull();
    }

    @Test
    void nullFluidIsNeitherFuelNorCoolant() {
        assertThat(FuelEngineFuels.lookup(null)).isNull();
        assertThat(FuelEngineFuels.isFuel(null)).isFalse();
        assertThat(FuelEngineCoolants.lookup(null)).isNull();
        assertThat(FuelEngineCoolants.isCoolant(null)).isFalse();
    }
}
