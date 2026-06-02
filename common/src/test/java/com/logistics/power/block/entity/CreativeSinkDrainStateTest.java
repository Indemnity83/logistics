package com.logistics.power.block.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CreativeSinkDrainStateTest {
    @Test
    void newState_usesDefaultDrainRate() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();

        assertThat(state.index()).isEqualTo(4);
        assertThat(state.drainRate()).isEqualTo(5);
        assertThat(state.networkDemandPerTick()).isEqualTo(5);
        assertThat(state.energyLastTick()).isZero();
        assertThat(state.energyThisTick()).isZero();
        assertThat(state.totalEnergyReceived()).isZero();
    }

    @Test
    void insert_acceptsUpToRemainingDemandForCurrentTick() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();

        assertThat(state.insert(3, false)).isEqualTo(3);
        assertThat(state.insert(10, false)).isEqualTo(2);

        assertThat(state.energyThisTick()).isEqualTo(5);
        assertThat(state.networkDemandPerTick()).isZero();
    }

    @Test
    void insert_simulationDoesNotConsumeDemand() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();

        assertThat(state.insert(3, true)).isEqualTo(3);

        assertThat(state.energyThisTick()).isZero();
        assertThat(state.networkDemandPerTick()).isEqualTo(5);
    }

    @Test
    void tick_movesCurrentTickIntoLastTickAndTotalThenResetsDemand() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();
        state.insert(4, false);

        state.tick();

        assertThat(state.energyLastTick()).isEqualTo(4);
        assertThat(state.energyThisTick()).isZero();
        assertThat(state.totalEnergyReceived()).isEqualTo(4);
        assertThat(state.networkDemandPerTick()).isEqualTo(5);
    }

    @Test
    void tick_saturatesTotalEnergyAtLongMaxValue() {
        CreativeSinkDrainState state = new CreativeSinkDrainState(new long[]{Long.MAX_VALUE});
        state.insert(Long.MAX_VALUE - 1, false);
        state.tick();
        state.insert(2, false);

        state.tick();

        assertThat(state.totalEnergyReceived()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void cycle_advancesAndWrapsDrainRates() {
        CreativeSinkDrainState state = new CreativeSinkDrainState(new long[]{1, 2});

        assertThat(state.cycle()).isEqualTo(2);
        assertThat(state.index()).isEqualTo(1);

        assertThat(state.cycle()).isEqualTo(1);
        assertThat(state.index()).isZero();
    }

    @Test
    void setUnlimited_selectsLastConfiguredDrainRate() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();

        state.setUnlimited();

        assertThat(state.index()).isEqualTo(CreativeSinkDrainState.DEFAULT_DRAIN_RATES.length - 1);
        assertThat(state.drainRate()).isEqualTo(Long.MAX_VALUE);
        assertThat(state.networkDemandPerTick()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void restore_acceptsValidIndex() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();

        state.restore(10);

        assertThat(state.index()).isEqualTo(10);
        assertThat(state.drainRate()).isEqualTo(15);
    }

    @Test
    void restore_clampsNegativeIndexToDefault() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();
        state.restore(10);

        state.restore(-1);

        assertThat(state.index()).isEqualTo(4);
        assertThat(state.drainRate()).isEqualTo(5);
    }

    @Test
    void restore_clampsOutOfRangeIndexToDefault() {
        CreativeSinkDrainState state = new CreativeSinkDrainState();
        state.restore(10);

        state.restore(CreativeSinkDrainState.DEFAULT_DRAIN_RATES.length);

        assertThat(state.index()).isEqualTo(4);
        assertThat(state.drainRate()).isEqualTo(5);
    }

    @Test
    void constructor_rejectsEmptyDrainRates() {
        assertThatThrownBy(() -> new CreativeSinkDrainState(new long[]{}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("drainRates");
    }

    @Test
    void constructor_defensivelyCopiesDrainRates() {
        long[] drainRates = {5, 10};
        CreativeSinkDrainState state = new CreativeSinkDrainState(drainRates);

        drainRates[0] = 999;

        assertThat(state.drainRate()).isEqualTo(5);
    }
}
