package com.logistics.automation.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The quarry arm is interpolated in wall-clock time, so the server's blocks-per-tick arm speed
 * has to be converted with the rate the level is actually ticking at.
 */
class LaserQuarryRenderStateTest extends MinecraftTestEnvironment {
    private static final long STEP_NANOS = 100_000_000L; // 0.1s, the interpolation delta clamp
    private static final float STEP_SECONDS = 0.1f;
    private static final float ARM_SPEED_PER_TICK = 0.1f;
    private static final float FAR_AWAY = 100f;
    private static final float TOLERANCE = 1e-4f;

    // Interpolation state is cached statically per position; give every case its own quarry.
    private static final AtomicInteger NEXT_POS = new AtomicInteger();

    @Test
    void armSpeedTracksTheLevelTickRate() {
        float atTwentyTps = distanceMovedInOneStep(20f);
        float atFortyTps = distanceMovedInOneStep(40f);
        float atTenTps = distanceMovedInOneStep(10f);

        assertThat(atTwentyTps).isCloseTo(ARM_SPEED_PER_TICK * 20f * STEP_SECONDS, within(TOLERANCE));
        assertThat(atFortyTps).isCloseTo(ARM_SPEED_PER_TICK * 40f * STEP_SECONDS, within(TOLERANCE));
        assertThat(atTenTps).isCloseTo(ARM_SPEED_PER_TICK * 10f * STEP_SECONDS, within(TOLERANCE));
    }

    @Test
    void frozenTickingHoldsAnArmCaughtMidTraverse() {
        LaserQuarryRenderState state = stateAtFreshPosition();
        state.tickRate = 20f;
        state.syncedArmSpeed = ARM_SPEED_PER_TICK;

        long now = 1_000_000_000L;
        state.updateClientInterpolation(now); // first update snaps to the server position
        state.serverArmX = FAR_AWAY;

        now += STEP_NANOS;
        state.updateClientInterpolation(now);
        float caughtMidTraverse = state.renderArmX;
        assertThat(caughtMidTraverse).isGreaterThan(0f).isLessThan(FAR_AWAY);

        state.tickingFrozen = true;
        for (int i = 0; i < 5; i++) {
            now += STEP_NANOS;
            state.updateClientInterpolation(now);
        }

        assertThat(state.renderArmX).isCloseTo(caughtMidTraverse, within(TOLERANCE));

        // Unfreezing must resume the traverse rather than leaving the arm stalled.
        state.tickingFrozen = false;
        now += STEP_NANOS;
        state.updateClientInterpolation(now);
        assertThat(state.renderArmX).isGreaterThan(caughtMidTraverse);
    }

    private float distanceMovedInOneStep(float tickRate) {
        LaserQuarryRenderState state = stateAtFreshPosition();
        state.tickRate = tickRate;
        state.syncedArmSpeed = ARM_SPEED_PER_TICK;

        long now = 1_000_000_000L;
        state.updateClientInterpolation(now); // first update snaps to the server position
        state.serverArmX = FAR_AWAY;

        state.updateClientInterpolation(now + STEP_NANOS);
        return state.renderArmX;
    }

    private LaserQuarryRenderState stateAtFreshPosition() {
        BlockPos pos = new BlockPos(NEXT_POS.incrementAndGet(), 64, 0);
        LaserQuarryRenderState.clearInterpolationCache(pos);

        LaserQuarryRenderState state = new LaserQuarryRenderState();
        state.quarryPos = pos;
        state.serverArmX = 0f;
        state.serverArmY = 0f;
        state.serverArmZ = 0f;
        return state;
    }
}
