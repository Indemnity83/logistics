package com.logistics.automation.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.automation.render.LaserQuarryBlockEntityRenderer.InterpolationState;
import org.junit.jupiter.api.Test;

/**
 * The quarry arm is interpolated in wall-clock time, so the server's blocks-per-tick arm speed has to be
 * converted with the rate the level is actually ticking at — and held still when the level is frozen,
 * since the wall clock keeps running either way.
 *
 * <p>This branch has no render state, so the interpolation lives in the renderer and is driven here
 * through its package-private overload with an explicit timestamp, rather than by sleeping.
 */
class LaserQuarryArmInterpolationTest {

    private static final long STEP_NANOS = 100_000_000L; // 0.1s, the interpolation delta clamp
    private static final float STEP_SECONDS = 0.1f;
    private static final float ARM_SPEED_PER_TICK = 0.1f;
    private static final float FAR_AWAY = 100f;
    private static final float TOLERANCE = 1e-4f;

    // A zero timestamp reads as "not yet initialized", so the clock starts at a non-zero base.
    private static final long BASE_NANOS = STEP_NANOS;

    /** A state already initialized at the origin, with its clock started. */
    private static InterpolationState settledAtOrigin() {
        InterpolationState interp = new InterpolationState();
        step(interp, 0f, ARM_SPEED_PER_TICK, 20f, false, BASE_NANOS); // snaps and starts the clock
        return interp;
    }

    private static void step(
            InterpolationState interp,
            float targetX,
            float armSpeed,
            float tickRate,
            boolean frozen,
            long nowNanos) {
        LaserQuarryBlockEntityRenderer.updateClientInterpolation(
                interp, targetX, 0f, 0f, armSpeed, tickRate, frozen, nowNanos);
    }

    private static float distanceMovedInOneStep(float tickRate) {
        InterpolationState interp = settledAtOrigin();
        step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, tickRate, false, BASE_NANOS + STEP_NANOS);
        return interp.renderArmX;
    }

    @Test
    void armSpeedTracksTheLevelTickRate() {
        assertThat(distanceMovedInOneStep(20f)).isCloseTo(ARM_SPEED_PER_TICK * 20f * STEP_SECONDS, within(TOLERANCE));
        assertThat(distanceMovedInOneStep(40f)).isCloseTo(ARM_SPEED_PER_TICK * 40f * STEP_SECONDS, within(TOLERANCE));
        assertThat(distanceMovedInOneStep(10f)).isCloseTo(ARM_SPEED_PER_TICK * 10f * STEP_SECONDS, within(TOLERANCE));
    }

    @Test
    void frozenTickingHoldsAnArmCaughtMidTraverse() {
        InterpolationState interp = settledAtOrigin();

        // One unfrozen step puts the arm mid-traverse: an arm already settled on its target would look
        // correct whether or not the freeze is honoured, so it could not tell the two apart.
        step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 20f, false, BASE_NANOS + STEP_NANOS);
        float caughtMidTraverse = interp.renderArmX;
        assertThat(caughtMidTraverse).isGreaterThan(0f).isLessThan(FAR_AWAY);

        for (int frame = 2; frame <= 6; frame++) {
            step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 20f, true, BASE_NANOS + STEP_NANOS * frame);
        }

        assertThat(interp.renderArmX).isCloseTo(caughtMidTraverse, within(TOLERANCE));
    }

    @Test
    void unfreezingResumesSmoothlyRatherThanJumping() {
        InterpolationState interp = settledAtOrigin();
        step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 20f, false, BASE_NANOS + STEP_NANOS);
        float beforeFreeze = interp.renderArmX;

        // Frozen frames still advance the clock, so the first unfrozen frame covers one step, not five.
        for (int frame = 2; frame <= 6; frame++) {
            step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 20f, true, BASE_NANOS + STEP_NANOS * frame);
        }
        step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 20f, false, BASE_NANOS + STEP_NANOS * 7);

        assertThat(interp.renderArmX - beforeFreeze)
                .isCloseTo(ARM_SPEED_PER_TICK * 20f * STEP_SECONDS, within(TOLERANCE));
    }

    @Test
    void aStoppedTickRateHoldsTheArmStill() {
        InterpolationState interp = settledAtOrigin();

        step(interp, FAR_AWAY, ARM_SPEED_PER_TICK, 0f, false, BASE_NANOS + STEP_NANOS);

        assertThat(interp.renderArmX).isCloseTo(0f, within(TOLERANCE));
    }

    @Test
    void anArmWithinReachStillSnapsToTheServerPosition() {
        InterpolationState interp = settledAtOrigin();

        // Target closer than one step of travel: the arm lands exactly on it rather than overshooting.
        step(interp, 0.01f, ARM_SPEED_PER_TICK, 20f, false, BASE_NANOS + STEP_NANOS);

        assertThat(interp.renderArmX).isCloseTo(0.01f, within(TOLERANCE));
    }
}
