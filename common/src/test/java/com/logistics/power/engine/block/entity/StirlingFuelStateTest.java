package com.logistics.power.engine.block.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StirlingFuelStateTest {
    @Test
    void newState_isNotBurningAndHasNoFuelTimers() {
        StirlingFuelState state = new StirlingFuelState();

        assertThat(state.isBurning()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isZero();
    }

    @Test
    void ignite_setsBurnAndFuelTimers() {
        StirlingFuelState state = new StirlingFuelState();

        state.ignite(200);

        assertThat(state.isBurning()).isTrue();
        assertThat(state.burnTime()).isEqualTo(200);
        assertThat(state.fuelTime()).isEqualTo(200);
    }

    @Test
    void ignite_clampsNegativeBurnTicksToZero() {
        StirlingFuelState state = new StirlingFuelState();

        state.ignite(-10);

        assertThat(state.isBurning()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isZero();
    }

    @Test
    void burn_decrementsBurnTimeAndStaysBurningUntilZero() {
        StirlingFuelState state = new StirlingFuelState();
        state.ignite(2);

        assertThat(state.burn()).isTrue();
        assertThat(state.burnTime()).isEqualTo(1);
        assertThat(state.fuelTime()).isEqualTo(2);

        assertThat(state.burn()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isEqualTo(2);
    }

    @Test
    void burn_onEmptyStateDoesNothing() {
        StirlingFuelState state = new StirlingFuelState();

        assertThat(state.burn()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isZero();
    }

    @Test
    void extinguish_clearsBurnAndFuelTimers() {
        StirlingFuelState state = new StirlingFuelState();
        state.ignite(200);

        state.extinguish();

        assertThat(state.isBurning()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isZero();
    }

    @Test
    void restore_rehydratesExistingTimers() {
        StirlingFuelState state = new StirlingFuelState();

        state.restore(40, 200);

        assertThat(state.isBurning()).isTrue();
        assertThat(state.burnTime()).isEqualTo(40);
        assertThat(state.fuelTime()).isEqualTo(200);
    }

    @Test
    void restore_clampsNegativeTimersToZero() {
        StirlingFuelState state = new StirlingFuelState();

        state.restore(-40, -200);

        assertThat(state.isBurning()).isFalse();
        assertThat(state.burnTime()).isZero();
        assertThat(state.fuelTime()).isZero();
    }
}
