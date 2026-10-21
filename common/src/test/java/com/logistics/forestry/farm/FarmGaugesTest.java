package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.forestry.farm.FarmGauges.Lamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Farm screen gauges")
class FarmGaugesTest {

    @Test
    @DisplayName("bar fill is empty at zero, full at capacity, and proportional between")
    void fillTracksAmount() {
        assertThat(FarmGauges.fillPixels(0, 20_000, 52)).isZero();
        assertThat(FarmGauges.fillPixels(10_000, 20_000, 52)).isEqualTo(26);
        assertThat(FarmGauges.fillPixels(20_000, 20_000, 52)).isEqualTo(52);
        assertThat(FarmGauges.fillPixels(5, 8, 30)).isEqualTo(18);
    }

    @Test
    @DisplayName("bar fill is clamped and safe without a capacity")
    void fillIsClamped() {
        assertThat(FarmGauges.fillPixels(25, 20, 30)).isEqualTo(30);
        assertThat(FarmGauges.fillPixels(-5, 20, 30)).isZero();
        assertThat(FarmGauges.fillPixels(5, 0, 30)).isZero();
        assertThat(FarmGauges.fillPixels(4_000_000_000L, 4_000_000_001L, 52)).isEqualTo(51);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"WORKING", "IDLE"})
    @DisplayName("lamp is green while the farm works or has nothing ripe")
    void lampIsGreenWhenAllIsWell(FarmStatus status) {
        assertThat(FarmGauges.lamp(status)).isEqualTo(Lamp.GREEN);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"WAITING_FOR_SEEDS", "INVENTORY_FULL", "PAUSED"})
    @DisplayName("lamp is amber while the farm needs attention but is not stopped by a fault")
    void lampIsAmberForWarnings(FarmStatus status) {
        assertThat(FarmGauges.lamp(status)).isEqualTo(Lamp.AMBER);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"NO_POWER", "BLOCKED"})
    @DisplayName("lamp is red when the farm cannot work")
    void lampIsRedForFaults(FarmStatus status) {
        assertThat(FarmGauges.lamp(status)).isEqualTo(Lamp.RED);
    }
}
