package com.logistics.core.lib.fluids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fluid unit conversion")
class FluidUnitsTest {

    @Test
    @DisplayName("converts millibuckets to native units and back with the platform factor")
    void roundTrip() {
        assertThat(FluidUnits.mb(250, 81)).isEqualTo(20_250);
        assertThat(FluidUnits.toMillibuckets(20_250, 81)).isEqualTo(250);
        assertThat(FluidUnits.mb(250, 1)).isEqualTo(250);
    }

    @Test
    @DisplayName("toMillibuckets rounds down")
    void roundsDown() {
        assertThat(FluidUnits.toMillibuckets(80, 81)).isZero();
        assertThat(FluidUnits.toMillibuckets(162, 81)).isEqualTo(2);
    }

    @Test
    @DisplayName("toMillibucketsCeil rounds up so a partial mB is never counted as still held")
    void ceilRoundsUp() {
        assertThat(FluidUnits.toMillibucketsCeil(80, 81)).isEqualTo(1);
        assertThat(FluidUnits.toMillibucketsCeil(162, 81)).isEqualTo(2);
        assertThat(FluidUnits.toMillibucketsCeil(0, 81)).isZero();
        // One cauldron level: a third of a bucket is 27000 droplets, i.e. 333⅓ mB.
        assertThat(FluidUnits.toMillibucketsCeil(27_000, 81)).isEqualTo(334);
    }

    @Test
    @DisplayName("a non-positive factor is rejected instead of dividing by zero")
    void rejectsNonPositiveFactor() {
        assertThatThrownBy(() -> FluidUnits.toMillibucketsCeil(100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FluidUnits.toMillibuckets(100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FluidUnits.toMillibuckets(100, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FluidUnits.mb(100, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("overflowing the native range throws rather than silently wrapping")
    void overflowThrows() {
        assertThatThrownBy(() -> FluidUnits.mb(Long.MAX_VALUE, 81)).isInstanceOf(ArithmeticException.class);
    }
}
