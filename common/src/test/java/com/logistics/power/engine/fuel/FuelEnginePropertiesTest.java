package com.logistics.power.engine.fuel;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.resource.ResourceId;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
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
    void fuelTiersByConventionalTag() {
        assertThat(FuelEngineFuels.forTags(inTags("fuel"))).isEqualTo(new FuelEngineFuel(150_000L, 2.25));
        assertThat(FuelEngineFuels.forTags(inTags("biofuel"))).isEqualTo(new FuelEngineFuel(80_000L, 1.5));
        assertThat(FuelEngineFuels.forTags(inTags("crude_oil"))).isEqualTo(new FuelEngineFuel(40_000L, 3.0));
        assertThat(FuelEngineFuels.forTags(inTags("oil"))).isEqualTo(new FuelEngineFuel(40_000L, 3.0));
        assertThat(FuelEngineFuels.forTags(inTags("diesel", "ethanol", "plantoil"))).isNull();
        assertThat(FuelEngineFuels.forTags(inTags())).isNull();
    }

    @Test
    void fluidInSeveralFuelTagsBurnsAsTheBest() {
        assertThat(FuelEngineFuels.forTags(inTags("oil", "biofuel", "fuel")))
                .isEqualTo(new FuelEngineFuel(150_000L, 2.25));
        assertThat(FuelEngineFuels.forTags(inTags("crude_oil", "biofuel")))
                .isEqualTo(new FuelEngineFuel(80_000L, 1.5));
    }

    private static Predicate<TagKey<Fluid>> inTags(String... paths) {
        Set<String> ids = Arrays.stream(paths).map(path -> "c:" + path).collect(Collectors.toSet());
        return tag -> ids.contains(ResourceId.wrap(tag.location()).toString());
    }

    @Test
    void nullFluidIsNeitherFuelNorCoolant() {
        assertThat(FuelEngineFuels.lookup(null)).isNull();
        assertThat(FuelEngineFuels.isFuel(null)).isFalse();
        assertThat(FuelEngineCoolants.lookup(null)).isNull();
        assertThat(FuelEngineCoolants.isCoolant(null)).isFalse();
    }
}
