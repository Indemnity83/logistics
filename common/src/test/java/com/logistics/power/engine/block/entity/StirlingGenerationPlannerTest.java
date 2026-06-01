package com.logistics.power.engine.block.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class StirlingGenerationPlannerTest {
    private static final double TOLERANCE = 0.0001;

    @Test
    void outputPower_scalesBetweenConfiguredBoundsByTemperature() {
        StirlingGenerationPlanner planner = planner();

        assertThat(planner.outputPower(20, 20, 3, 10)).isEqualTo(3);
        assertThat(planner.outputPower(85, 20, 3, 10)).isEqualTo(7);
        assertThat(planner.outputPower(150, 20, 3, 10)).isEqualTo(10);
    }

    @Test
    void outputPower_capsTemperatureRatioAtTarget() {
        StirlingGenerationPlanner planner = planner();

        assertThat(planner.outputPower(250, 20, 3, 10)).isEqualTo(10);
    }

    @Test
    void generate_addsWholeEnergyAndKeepsFractionalCarry() {
        StirlingGenerationPlanner planner = planner();

        long first = planner.generate(150, 0, 10_000, 3.25, 3.25);
        long second = planner.generate(150, first, 10_000, 3.25, 3.25);

        assertThat(first).isEqualTo(3);
        assertThat(second).isEqualTo(3);
        assertThat(planner.currentGeneration()).isCloseTo(3.25, within(TOLERANCE));
        assertThat(planner.generationCarry()).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    void generate_capsAddedEnergyToAvailableSpaceAndKeepsRemainder() {
        StirlingGenerationPlanner planner = planner();

        long generated = planner.generate(150, 8, 10, 3.25, 3.25);

        assertThat(generated).isEqualTo(2);
        assertThat(planner.generationCarry()).isCloseTo(1.25, within(TOLERANCE));
    }

    @Test
    void generate_resetsCarryWhenBufferIsAlreadyFull() {
        StirlingGenerationPlanner planner = planner();

        long generated = planner.generate(150, 10, 10, 3.25, 3.25);

        assertThat(generated).isZero();
        assertThat(planner.generationCarry()).isZero();
    }

    @Test
    void reset_restoresDefaultGenerationAndClearsState() {
        StirlingGenerationPlanner planner = planner();
        planner.generate(20, 0, 10_000, 3, 10);

        planner.reset();

        assertThat(planner.currentGeneration()).isCloseTo(3.0, within(TOLERANCE));
        assertThat(planner.generationCarry()).isZero();
        assertThat(planner.pidIntegral()).isZero();
    }

    @Test
    void restore_rehydratesPersistentGenerationState() {
        StirlingGenerationPlanner planner = planner();

        planner.restore(6.5, 0.75, 12.25);

        assertThat(planner.currentGeneration()).isCloseTo(6.5, within(TOLERANCE));
        assertThat(planner.generationCarry()).isCloseTo(0.75, within(TOLERANCE));
        assertThat(planner.pidIntegral()).isCloseTo(12.25, within(TOLERANCE));
    }

    private static StirlingGenerationPlanner planner() {
        return new StirlingGenerationPlanner(0.2, 0.0002, 0.3, 150, 3);
    }
}
