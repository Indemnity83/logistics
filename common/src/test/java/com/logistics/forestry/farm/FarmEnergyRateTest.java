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
            assertThat(rate.tenthsPerTick()).as("tick " + tick).isEqualTo(50);
        }
    }

    @Test
    @DisplayName("a farm that stops spending reads 0 once the window has passed")
    void idleReadsZero() {
        FarmEnergyRate rate = new FarmEnergyRate();
        for (int tick = 0; tick < FarmEnergyRate.WINDOW; tick++) {
            rate.record(100);
        }
        assertThat(rate.tenthsPerTick()).isEqualTo(1_000);

        for (int tick = 0; tick < FarmEnergyRate.WINDOW - 1; tick++) {
            rate.record(0);
        }
        assertThat(rate.tenthsPerTick()).as("one busy tick left in the window").isEqualTo(50);
        rate.record(0);
        assertThat(rate.tenthsPerTick()).isZero();
    }

    @Test
    @DisplayName("upkeep below 1 RF/t, paid 1 RF every few ticks, reads in tenths")
    void upkeepBelowOneRf() {
        FarmEnergyRate rate = new FarmEnergyRate();
        // 4 crop Plots at 0.1 RF/t: 1 RF every 2.5 ticks, 8 RF a window.
        for (int tick = 0; tick < 3 * FarmEnergyRate.WINDOW; tick++) {
            rate.record(tick % 5 == 0 || tick % 5 == 3 ? 1 : 0);
        }

        assertThat(rate.tenthsPerTick()).isEqualTo(4);
    }

    @Test
    @DisplayName("a fresh farm reads 0, and the average rounds to the nearest tenth")
    void freshAndRounded() {
        FarmEnergyRate rate = new FarmEnergyRate();
        assertThat(rate.tenthsPerTick()).isZero();

        rate.record(30);
        assertThat(rate.tenthsPerTick()).as("30 / 20 = 1.5").isEqualTo(15);
        rate.record(1);
        assertThat(rate.tenthsPerTick()).as("31 / 20 = 1.55").isEqualTo(16);
    }
}
