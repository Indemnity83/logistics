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

    @Test
    @DisplayName("lamp is green while working and amber while idle or paused")
    void lampForBenignStatuses() {
        assertThat(FarmGauges.lamp(FarmStatus.WORKING)).isEqualTo(Lamp.GREEN);
        assertThat(FarmGauges.lamp(FarmStatus.IDLE)).isEqualTo(Lamp.AMBER);
        assertThat(FarmGauges.lamp(FarmStatus.PAUSED)).isEqualTo(Lamp.AMBER);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"WORKING", "IDLE", "PAUSED"})
    @DisplayName("lamp is red for every problem status")
    void lampForProblems(FarmStatus status) {
        assertThat(FarmGauges.lamp(status)).isEqualTo(Lamp.RED);
    }
}
