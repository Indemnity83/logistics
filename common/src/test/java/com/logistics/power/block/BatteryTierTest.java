package com.logistics.power.block;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pins each tier's shipped storage numbers, which the config defaults supply. */
@DisplayName("Battery tiers")
class BatteryTierTest {

    @Test
    @DisplayName("capacity steps x4 and throughput x2 per tier")
    void tiersUseExpectedNumbers() {
        assertThat(BatteryTier.BASIC.capacity()).isEqualTo(100_000);
        assertThat(BatteryTier.CONDUCTIVE.capacity()).isEqualTo(400_000);
        assertThat(BatteryTier.RESONANT.capacity()).isEqualTo(1_600_000);
        assertThat(BatteryTier.DEEP.capacity()).isEqualTo(6_400_000);

        assertThat(BatteryTier.BASIC.maxIo()).isEqualTo(1_000);
        assertThat(BatteryTier.CONDUCTIVE.maxIo()).isEqualTo(2_000);
        assertThat(BatteryTier.RESONANT.maxIo()).isEqualTo(4_000);
        assertThat(BatteryTier.DEEP.maxIo()).isEqualTo(8_000);

        assertThat(BatteryTier.BASIC.outputPerSide()).isEqualTo(200);
        assertThat(BatteryTier.CONDUCTIVE.outputPerSide()).isEqualTo(400);
        assertThat(BatteryTier.RESONANT.outputPerSide()).isEqualTo(800);
        assertThat(BatteryTier.DEEP.outputPerSide()).isEqualTo(1_600);
    }

    /**
     * The pre-tier Battery shipped at 100,000 RF / 1,000 RF-t / 200 RF-t, and
     * {@code logistics:power/battery} aliases to Basic — so if these drift, every saved battery in
     * every existing world is silently re-rated.
     */
    @Test
    @DisplayName("Basic still carries the untiered Battery's numbers, which the alias depends on")
    void basicMatchesThePreTierBattery() {
        assertThat(BatteryTier.BASIC.capacity()).isEqualTo(100_000);
        assertThat(BatteryTier.BASIC.maxIo()).isEqualTo(1_000);
        assertThat(BatteryTier.BASIC.outputPerSide()).isEqualTo(200);
    }
}
