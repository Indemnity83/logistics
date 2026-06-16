package com.logistics.pipe.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidColumnGeometry")
class FluidColumnGeometryTest {

    // Simple block-space coordinates for readability (treat as 0..16 px).
    private static final float CORE_BOTTOM = 4;
    private static final float CORE_TOP = 12;

    @Test
    @DisplayName("the surface rises linearly over the core span with fill")
    void surfaceRisesWithFill() {
        assertThat(FluidColumnGeometry.surface(0f, CORE_BOTTOM, CORE_TOP)).isEqualTo(CORE_BOTTOM);
        assertThat(FluidColumnGeometry.surface(0.5f, CORE_BOTTOM, CORE_TOP)).isEqualTo(8f); // mid-core
        assertThat(FluidColumnGeometry.surface(1f, CORE_BOTTOM, CORE_TOP)).isEqualTo(CORE_TOP);
    }

    @Test
    @DisplayName("the surface clamps out-of-range fill to the core span")
    void surfaceClamps() {
        assertThat(FluidColumnGeometry.surface(-1f, CORE_BOTTOM, CORE_TOP)).isEqualTo(CORE_BOTTOM);
        assertThat(FluidColumnGeometry.surface(2f, CORE_BOTTOM, CORE_TOP)).isEqualTo(CORE_TOP);
    }

    @Test
    @DisplayName("the column half-width is area-proportional (∝ sqrt of fill)")
    void halfWidthIsAreaProportional() {
        float max = 0.25f;
        assertThat(FluidColumnGeometry.halfWidth(0f, max)).isEqualTo(0f); // empty: no column
        assertThat(FluidColumnGeometry.halfWidth(1f, max)).isEqualTo(max); // full: spans the cross-section
        // Half volume reads as ~71% width, since area (∝ width^2) tracks the fill.
        assertThat(FluidColumnGeometry.halfWidth(0.5f, max)).isEqualTo(max * (float) Math.sqrt(0.5));
    }

    @Test
    @DisplayName("the column half-width clamps out-of-range fill")
    void halfWidthClamps() {
        float max = 0.25f;
        assertThat(FluidColumnGeometry.halfWidth(-1f, max)).isEqualTo(0f);
        assertThat(FluidColumnGeometry.halfWidth(2f, max)).isEqualTo(max);
    }
}
