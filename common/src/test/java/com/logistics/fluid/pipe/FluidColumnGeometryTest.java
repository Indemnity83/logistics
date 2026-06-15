package com.logistics.fluid.pipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidColumnGeometry.of")
class FluidColumnGeometryTest {

    // Simple block-space coordinates for readability (treat as 0..16 px).
    private static final float CORE_BOTTOM = 4;
    private static final float CORE_TOP = 12;
    private static final float FLOOR = 0;
    private static final float CEILING = 16;

    private static FluidColumnGeometry.Column at(float ratio, boolean down, boolean up) {
        return FluidColumnGeometry.of(ratio, down, up, CORE_BOTTOM, CORE_TOP, FLOOR, CEILING);
    }

    @Test
    @DisplayName("the surface height depends only on fill, not on which arms are connected")
    void surfaceMatchesAcrossShapes() {
        float horizontal = at(0.5f, false, false).surface();
        float upDown = at(0.5f, true, true).surface();
        assertThat(upDown).isEqualTo(horizontal).isEqualTo(8f); // mid-core for both
    }

    @Test
    @DisplayName("a down arm is full whenever the pipe holds fluid")
    void downArmFullWhenWet() {
        assertThat(at(0.01f, true, false).bottom()).isEqualTo(FLOOR); // reaches the floor even at a trickle
        assertThat(at(0.01f, false, false).bottom()).isEqualTo(CORE_BOTTOM); // no down arm: starts at the core
    }

    @Test
    @DisplayName("an up arm fills only once the pipe is full")
    void upArmFillsOnlyWhenFull() {
        FluidColumnGeometry.Column almost = at(0.99f, false, true);
        assertThat(almost.top()).isEqualTo(almost.surface()); // not yet — still at the core surface
        assertThat(at(1.0f, false, true).top()).isEqualTo(CEILING); // pops to full
    }

    @Test
    @DisplayName("without an up arm, a full pipe tops out at the core")
    void fullWithoutUpArmStopsAtCore() {
        assertThat(at(1.0f, false, false).top()).isEqualTo(CORE_TOP);
    }
}
