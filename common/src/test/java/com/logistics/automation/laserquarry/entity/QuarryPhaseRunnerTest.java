package com.logistics.automation.laserquarry.entity;


import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryPhaseRunner")
class QuarryPhaseRunnerTest {

    @Test
    @DisplayName("custom bounds reset restarts clearing from the first frame block")
    void customBoundsResetRestartsFullSequence() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        CompoundTag state = new CompoundTag();
        state.putInt("MiningX", 3);
        state.putInt("MiningY", 4);
        state.putInt("MiningZ", 5);
        state.putFloat("BreakProgress", 12f);
        state.putBoolean("MiningFinished", true);
        state.putString("CurrentPhase", QuarryPhase.MINING.name());
        state.putInt("FrameBuildIndex", 42);
        runner.load(state);

        runner.onCustomBoundsSet();

        CompoundTag saved = new CompoundTag();
        runner.save(saved);
        assertThat(saved.getString("CurrentPhase")).isEqualTo(QuarryPhase.CLEARING.name());
        assertThat(saved.getInt("FrameBuildIndex")).isZero();
        assertThat(saved.getInt("MiningX")).isZero();
        assertThat(saved.getInt("MiningY")).isZero();
        assertThat(saved.getInt("MiningZ")).isZero();
        assertThat(saved.getFloat("BreakProgress")).isZero();
        assertThat(saved.getBoolean("MiningFinished")).isFalse();
        assertThat(runner.getCurrentTarget()).isNull();
        assertThat(runner.getCurrentBreakTime()).isEqualTo(-1f);
    }
}
