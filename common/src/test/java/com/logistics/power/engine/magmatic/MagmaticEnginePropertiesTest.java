package com.logistics.power.engine.magmatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Property-math for the Magmatic Engine profile: burn duration, stage outputs, the continuous
 * output↔temperature curve, and the ignition-admission requirement. Pure — no Minecraft bootstrap
 * (lava-tag recognition is covered by {@code MagmaticEngineFuelsTest} / the game test).
 */
class MagmaticEnginePropertiesTest {

    // Config defaults: base 10 RF/t, 40k buffer, 4,000 mB tank, 20,000-tick lava bucket.
    private static final MagmaticEngineProfile PROFILE = MagmaticEngineProfile.of(10, 40_000, 4_000, 20_000);

    @Test
    void batchBurnTicksFromBucketDuration() {
        assertThat(PROFILE.batchBurnTicks()).isEqualTo(2_000); // 20,000 * 100 / 1000
        assertThat(MagmaticEngineProfile.of(10, 40_000, 4_000, 20_000).bucketBurnTicks()).isEqualTo(20_000);
    }

    @Test
    void stageOutputsFromBaseRate() {
        assertThat(PROFILE.coldOutputPerTick()).isEqualTo(5L);
        assertThat(PROFILE.warmOutputPerTick()).isEqualTo(10L);
        assertThat(PROFILE.hotOutputPerTick()).isEqualTo(15L);
    }

    @Test
    void outputAtTemperatureEndpointsAndMidpoint() {
        assertThat(PROFILE.outputAtTemperature(0.0)).isEqualTo(5L);
        assertThat(PROFILE.outputAtTemperature(0.5)).isEqualTo(10L);
        assertThat(PROFILE.outputAtTemperature(1.0)).isEqualTo(15L);
    }

    @Test
    void outputAtTemperatureIsMonotonicNonDecreasing() {
        long previous = Long.MIN_VALUE;
        for (int i = 0; i <= 100; i++) {
            long out = PROFILE.outputAtTemperature(i / 100.0);
            assertThat(out).isGreaterThanOrEqualTo(previous).isBetween(5L, 15L);
            previous = out;
        }
    }

    @Test
    void outputAtTemperatureClampsOutOfRange() {
        assertThat(PROFILE.outputAtTemperature(-1.0)).isEqualTo(5L);
        assertThat(PROFILE.outputAtTemperature(2.0)).isEqualTo(15L);
    }

    @Test
    void maximumBatchPotentialIsHotOutputTimesDuration() {
        assertThat(PROFILE.maximumBatchPotentialRf()).isEqualTo(30_000L); // 15 * 2,000
    }

    @Test
    void sustainedHotProducesFullBucketPotential() {
        // 15 RF/t across a full 20,000-tick bucket (10 batches) = 300,000 RF ≈ 150 Kiln ops at 2,000 RF.
        long perBucket = PROFILE.hotOutputPerTick() * PROFILE.bucketBurnTicks();
        assertThat(perBucket).isEqualTo(300_000L);
        assertThat(perBucket / 2_000L).isEqualTo(150L);
    }

    @Test
    void displayBandFloorsAreHysteresisMidpoints() {
        assertThat(PROFILE.warmBandFloor()).isEqualTo(0.25, within(1e-9));
        assertThat(PROFILE.hotBandFloor()).isEqualTo(0.75, within(1e-9));
    }
}
