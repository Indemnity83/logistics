package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm energy rate")
class FarmEnergyRateTest {

    @Test
    @DisplayName("25 RF every 5th tick reads a steady 5 RF/t")
    void steadyActionsAverageOut() {
        FarmEnergyRate rate = new FarmEnergyRate();
        for (int tick = 0; tick < FarmEnergyRate.WINDOW; tick++) {
            rate.record(tick % 5 == 0 ? 25 : 0);
        }

        for (int tick = 0; tick < 3 * FarmEnergyRate.WINDOW; tick++) {
            rate.record(tick % 5 == 0 ? 25 : 0);
            assertThat(rate.perTick()).as("tick " + tick).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("a farm that stops spending reads 0 once the window has passed")
    void idleReadsZero() {
        FarmEnergyRate rate = new FarmEnergyRate();
        for (int tick = 0; tick < FarmEnergyRate.WINDOW; tick++) {
            rate.record(100);
        }
        assertThat(rate.perTick()).isEqualTo(100);

        for (int tick = 0; tick < FarmEnergyRate.WINDOW - 1; tick++) {
            rate.record(0);
        }
        assertThat(rate.perTick()).as("one busy tick left in the window").isEqualTo(5);
        rate.record(0);
        assertThat(rate.perTick()).isZero();
    }

    @Test
    @DisplayName("a fresh farm reads 0, and the average rounds to the nearest RF")
    void freshAndRounded() {
        FarmEnergyRate rate = new FarmEnergyRate();
        assertThat(rate.perTick()).isZero();

        rate.record(30);
        assertThat(rate.perTick()).as("30 / 20 = 1.5").isEqualTo(2);
        rate.record(0);
        rate.record(9);
        assertThat(rate.perTick()).as("39 / 20 = 1.95").isEqualTo(2);
    }
}
