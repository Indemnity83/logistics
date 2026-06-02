package com.logistics.power.engine.block.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CreativeOutputLevelsTest {
    @Test
    void newState_startsAtFirstOutputLevel() {
        CreativeOutputLevels levels = new CreativeOutputLevels();

        assertThat(levels.index()).isZero();
        assertThat(levels.outputRate()).isEqualTo(20);
        assertThat(levels.pistonSpeed()).isEqualTo(0.02F);
    }

    @Test
    void cycle_advancesToNextOutputLevel() {
        CreativeOutputLevels levels = new CreativeOutputLevels();

        long outputRate = levels.cycle();

        assertThat(outputRate).isEqualTo(40);
        assertThat(levels.index()).isEqualTo(1);
        assertThat(levels.outputRate()).isEqualTo(40);
        assertThat(levels.pistonSpeed()).isEqualTo(0.04F);
    }

    @Test
    void cycle_wrapsBackToFirstOutputLevel() {
        CreativeOutputLevels levels = new CreativeOutputLevels(new long[]{20, 40});

        levels.cycle();
        long outputRate = levels.cycle();

        assertThat(outputRate).isEqualTo(20);
        assertThat(levels.index()).isZero();
    }

    @Test
    void restore_acceptsValidIndex() {
        CreativeOutputLevels levels = new CreativeOutputLevels();

        levels.restore(3);

        assertThat(levels.index()).isEqualTo(3);
        assertThat(levels.outputRate()).isEqualTo(160);
        assertThat(levels.pistonSpeed()).isEqualTo(0.08F);
    }

    @Test
    void restore_clampsNegativeIndexToFirstLevel() {
        CreativeOutputLevels levels = new CreativeOutputLevels();

        levels.restore(-1);

        assertThat(levels.index()).isZero();
        assertThat(levels.outputRate()).isEqualTo(20);
    }

    @Test
    void restore_clampsOutOfRangeIndexToFirstLevel() {
        CreativeOutputLevels levels = new CreativeOutputLevels();

        levels.restore(CreativeOutputLevels.DEFAULT_LEVELS.length);

        assertThat(levels.index()).isZero();
        assertThat(levels.outputRate()).isEqualTo(20);
    }

    @Test
    void constructor_rejectsEmptyLevels() {
        assertThatThrownBy(() -> new CreativeOutputLevels(new long[]{}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("levels");
    }

    @Test
    void constructor_defensivelyCopiesLevels() {
        long[] configuredLevels = {20, 40};
        CreativeOutputLevels levels = new CreativeOutputLevels(configuredLevels);

        configuredLevels[0] = 999;

        assertThat(levels.outputRate()).isEqualTo(20);
    }
}
