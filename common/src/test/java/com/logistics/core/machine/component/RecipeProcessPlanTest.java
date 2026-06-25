package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecipeProcessPlanTest {

    @Test
    void pausesWhenEnergyBelowPerTickRate() {
        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(30, 100, 9, 10, true);

        assertThat(result.energySpent()).isEqualTo(30);
        assertThat(result.consumedEnergy()).isFalse();
        assertThat(result.lit()).isFalse();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void pausesWhenOutputCannotAccept() {
        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(30, 100, 1000, 10, false);

        assertThat(result.consumedEnergy()).isFalse();
        assertThat(result.lit()).isFalse();
    }

    @Test
    void spendsRfPerTickTowardRequirement() {
        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(30, 100, 1000, 10, true);

        assertThat(result.energySpent()).isEqualTo(40);
        assertThat(result.consumedEnergy()).isTrue();
        assertThat(result.lit()).isTrue();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void completesWhenSpentReachesRequirement() {
        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(90, 100, 1000, 10, true);

        assertThat(result.energySpent()).isEqualTo(100);
        assertThat(result.complete()).isTrue();
    }

    @Test
    void zeroRateNeverProcesses() {
        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(0, 100, 1000, 0, true);

        assertThat(result.consumedEnergy()).isFalse();
    }
}
