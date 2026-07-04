package com.logistics.pipe.block.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidPumpComponent.stepArm")
class FluidPumpArmTest extends MinecraftTestEnvironment {

    private static final float TOL = 1e-4f;

    @Test
    @DisplayName("descends one speed-step toward a lower target")
    void descendsTowardLowerTarget() {
        assertThat(FluidPumpComponent.stepArm(10f, 5f, 2f)).isCloseTo(8f, within(TOL));
    }

    @Test
    @DisplayName("ascends one speed-step toward a higher target")
    void ascendsTowardHigherTarget() {
        assertThat(FluidPumpComponent.stepArm(5f, 10f, 2f)).isCloseTo(7f, within(TOL));
    }

    @Test
    @DisplayName("clamps to the target instead of overshooting downward")
    void clampsWithoutOvershootingDown() {
        assertThat(FluidPumpComponent.stepArm(6f, 5f, 2f)).isEqualTo(5f);
    }

    @Test
    @DisplayName("clamps to the target instead of overshooting upward")
    void clampsWithoutOvershootingUp() {
        assertThat(FluidPumpComponent.stepArm(5f, 6f, 2f)).isEqualTo(6f);
    }

    @Test
    @DisplayName("lands exactly on the target when a step reaches it")
    void landsExactlyOnTarget() {
        assertThat(FluidPumpComponent.stepArm(7f, 5f, 2f)).isEqualTo(5f);
    }

    @Test
    @DisplayName("returns the same value when already at rest on the target")
    void unchangedAtRest() {
        assertThat(FluidPumpComponent.stepArm(5f, 5f, 2f)).isEqualTo(5f);
    }
}
