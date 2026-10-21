package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("the screen lamp shows the green sprite while the farm works or has nothing ripe")
    void lampIsGreenWhenAllIsWell(FarmStatus status) {
        assertThat(FarmGauges.lampSprite(status)).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"WAITING_FOR_SEEDS", "INVENTORY_FULL", "PAUSED"})
    @DisplayName("the screen lamp shows the amber sprite while the farm needs attention")
    void lampIsAmberForWarnings(FarmStatus status) {
        assertThat(FarmGauges.lampSprite(status)).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"NO_POWER", "BLOCKED"})
    @DisplayName("the screen lamp shows the red sprite when the farm cannot work")
    void lampIsRedForFaults(FarmStatus status) {
        assertThat(FarmGauges.lampSprite(status)).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(FarmStatus.class)
    @DisplayName("every status has a screen lamp sprite")
    void everyStatusHasASprite(FarmStatus status) {
        assertThat(FarmGauges.lampSprite(status)).isBetween(0, 2);
    }
}
