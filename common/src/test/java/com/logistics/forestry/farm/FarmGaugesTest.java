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

    @Test
    @DisplayName("the GUI lamp sprite follows the shared lamp colour: green, amber, red along the row")
    void lampSpriteFollowsTheColour() {
        assertThat(FarmGauges.lampSprite(FarmStatus.WORKING)).isZero();
        assertThat(FarmGauges.lampSprite(FarmStatus.IDLE)).isEqualTo(1);
        assertThat(FarmGauges.lampSprite(FarmStatus.NO_POWER)).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(FarmStatus.class)
    @DisplayName("every status has a GUI lamp sprite, shared by every status of its colour")
    void everyStatusHasASprite(FarmStatus status) {
        FarmStatus sameColour = switch (FarmLamp.forStatus(status)) {
            case GREEN -> FarmStatus.WORKING;
            case AMBER -> FarmStatus.IDLE;
            default -> FarmStatus.NO_POWER;
        };
        assertThat(FarmGauges.lampSprite(status)).isEqualTo(FarmGauges.lampSprite(sameColour));
    }
}
