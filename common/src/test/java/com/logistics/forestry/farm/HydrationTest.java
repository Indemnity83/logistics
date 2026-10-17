package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Irrigation water by climate")
class HydrationTest {

    @ParameterizedTest(name = "temperature {0}, precipitation {1} -> {2}")
    @CsvSource({
        // temperate biomes pay the temperature, floored at 0.8
        "1.0, true, 1.0",
        "0.8, true, 0.8",
        "0.5, true, 0.8",
        "-0.5, true, 0.8",
        "0.95, true, 0.95",
        // biomes without precipitation pay half again, capped at twice the base
        "0.8, false, 1.2",
        "1.2, false, 1.8",
        "2.0, false, 2.0",
        "2.0, true, 2.0",
        "3.0, false, 2.0"
    })
    void factor(float temperature, boolean precipitation, double expected) {
        assertThat(Hydration.factor(new FarmClimate(temperature, precipitation))).isCloseTo(expected, within(1e-6));
    }

    @Test
    @DisplayName("the neutral climate costs exactly the base")
    void neutral() {
        assertThat(Hydration.factor(FarmClimate.NEUTRAL)).isEqualTo(1.0);
        assertThat(Hydration.waterMb(100, FarmClimate.NEUTRAL)).isEqualTo(100);
    }

    @Test
    @DisplayName("the charged water is rounded to the nearest mB, never negative")
    void rounding() {
        // plains: 0.8
        assertThat(Hydration.waterMb(100, new FarmClimate(0.8f, true))).isEqualTo(80);
        // 0.95 x 33 = 31.35
        assertThat(Hydration.waterMb(33, new FarmClimate(0.95f, true))).isEqualTo(31);
        // 1.5 x 1 = 1.5 rounds half up
        assertThat(Hydration.waterMb(1, new FarmClimate(1.5f, true))).isEqualTo(2);
        // a hot, dry biome pays twice the shipped 100 mB
        assertThat(Hydration.waterMb(100, new FarmClimate(2f, false))).isEqualTo(200);
        assertThat(Hydration.waterMb(-10, FarmClimate.NEUTRAL)).isZero();
        assertThat(Hydration.waterMb(0, new FarmClimate(2f, false))).isZero();
    }
}
