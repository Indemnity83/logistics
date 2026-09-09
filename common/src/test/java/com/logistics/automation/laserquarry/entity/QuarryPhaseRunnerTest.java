package com.logistics.automation.laserquarry.entity;


import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryPhaseRunner")
class QuarryPhaseRunnerTest {

    /** Mid-mining state: a cursor part-way through the grid with break progress on a live target. */
    private static CompoundTag midMiningState() {
        CompoundTag state = new CompoundTag();
        state.putInt("MiningX", 3);
        state.putInt("MiningY", 4);
        state.putInt("MiningZ", 5);
        state.putFloat("BreakProgress", 12f);
        state.putBoolean("MiningFinished", true);
        state.putString("CurrentPhase", QuarryPhase.MINING.name());
        state.putInt("FrameBuildIndex", 42);
        state.putInt("FrameRepairIndex", 7);
        state.putInt("FrameScanIndex", 9);
        state.putInt("ClearanceIndex", 11);
        state.putIntArray("CurrentTarget", new int[] {3, 4, 5});
        return state;
    }

    @Test
    @DisplayName("phase state survives a save -> load round-trip")
    void phaseStateSurvivesSaveLoadRoundTrip() {
        QuarryPhaseRunner source = new QuarryPhaseRunner();
        source.load(midMiningState());

        CompoundTag saved = new CompoundTag();
        source.save(saved);

        QuarryPhaseRunner restored = new QuarryPhaseRunner();
        restored.load(saved);

        assertThat(restored.getPhase()).isEqualTo(QuarryPhase.MINING);
        assertThat(restored.getMiningX()).isEqualTo(3);
        assertThat(restored.getMiningY()).isEqualTo(4);
        assertThat(restored.getMiningZ()).isEqualTo(5);
        assertThat(restored.getBreakProgress()).isEqualTo(12f);
        assertThat(restored.isFinished()).isTrue();
        assertThat(restored.getCurrentTarget()).isEqualTo(new BlockPos(3, 4, 5));

        // No getters for the sweep cursors, so they are checked on the re-saved tag.
        CompoundTag resaved = new CompoundTag();
        restored.save(resaved);
        assertThat(resaved.getInt("FrameBuildIndex")).isEqualTo(42);
        assertThat(resaved.getInt("FrameRepairIndex")).isEqualTo(7);
        assertThat(resaved.getInt("FrameScanIndex")).isEqualTo(9);
        assertThat(resaved.getInt("ClearanceIndex")).isEqualTo(11);
    }

    @Test
    @DisplayName("break cost is deliberately not persisted — it is recomputed from the target block")
    void breakCostIsRecomputedRatherThanRestored() {
        QuarryPhaseRunner source = new QuarryPhaseRunner();
        source.load(midMiningState());

        CompoundTag saved = new CompoundTag();
        source.save(saved);

        assertThat(saved.contains("CurrentBreakTime")).isFalse();

        QuarryPhaseRunner restored = new QuarryPhaseRunner();
        restored.load(saved);
        // -1 is the "not yet costed" sentinel the break loop uses to re-derive the cost on the
        // next tick, so the progress already earned still applies to the block actually there.
        assertThat(restored.getCurrentBreakTime()).isEqualTo(-1f);
        assertThat(restored.getBreakProgress()).isEqualTo(12f);
    }

    @Test
    @DisplayName("custom bounds reset restarts clearing from the first frame block")
    void customBoundsResetRestartsFullSequence() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        runner.load(midMiningState());

        // Pinning the pre-reset state keeps the reset assertions honest: without this, every
        // assertion below would still hold if load() did nothing at all.
        assertThat(runner.getPhase()).isEqualTo(QuarryPhase.MINING);
        assertThat(runner.getMiningX()).isEqualTo(3);
        assertThat(runner.getBreakProgress()).isEqualTo(12f);
        assertThat(runner.isFinished()).isTrue();
        assertThat(runner.getCurrentTarget()).isEqualTo(new BlockPos(3, 4, 5));

        runner.onCustomBoundsSet();

        assertThat(runner.getPhase()).isEqualTo(QuarryPhase.CLEARING);
        assertThat(runner.getMiningX()).isZero();
        assertThat(runner.getMiningY()).isZero();
        assertThat(runner.getMiningZ()).isZero();
        assertThat(runner.getBreakProgress()).isZero();
        assertThat(runner.isFinished()).isFalse();
        assertThat(runner.getCurrentTarget()).isNull();
        assertThat(runner.getCurrentBreakTime()).isEqualTo(-1f);

        CompoundTag saved = new CompoundTag();
        runner.save(saved);
        assertThat(saved.getString("CurrentPhase")).isEqualTo(QuarryPhase.CLEARING.name());
        assertThat(saved.getInt("FrameBuildIndex")).isZero();
        assertThat(saved.getInt("FrameRepairIndex")).isZero();
        assertThat(saved.getInt("FrameScanIndex")).isZero();
        assertThat(saved.getInt("ClearanceIndex")).isZero();
    }
}
