package com.logistics.core.lib.tank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FluidColumn.settle")
class FluidColumnTest {

    @Test
    @DisplayName("liquids fill bottom-up")
    void liquidsFillBottomUp() {
        long[] caps = {100, 100, 100};
        assertThat(FluidColumn.settle(caps, 150, false)).containsExactly(100, 50, 0);
    }

    @Test
    @DisplayName("gases fill top-down")
    void gasesFillTopDown() {
        long[] caps = {100, 100, 100};
        assertThat(FluidColumn.settle(caps, 150, true)).containsExactly(0, 50, 100);
    }

    @Test
    @DisplayName("a full column fills every cell to capacity")
    void fullColumn() {
        long[] caps = {100, 50, 200};
        assertThat(FluidColumn.settle(caps, 350, false)).containsExactly(100, 50, 200);
    }

    @Test
    @DisplayName("zero total empties all cells")
    void zeroEmptiesAll() {
        long[] caps = {100, 100};
        assertThat(FluidColumn.settle(caps, 0, false)).containsExactly(0, 0);
    }

    @Test
    @DisplayName("rejects negative or over-capacity totals")
    void rejectsInvalidTotals() {
        long[] caps = {100, 100};
        assertThatThrownBy(() -> FluidColumn.settle(caps, -1, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FluidColumn.settle(caps, 201, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
