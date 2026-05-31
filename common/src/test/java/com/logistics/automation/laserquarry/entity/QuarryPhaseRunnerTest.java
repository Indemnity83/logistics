package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity.Phase;
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
        state.putString("CurrentPhase", Phase.MINING.name());
        state.putInt("FrameBuildIndex", 42);
        runner.load(state);

        runner.onCustomBoundsSet();

        CompoundTag saved = new CompoundTag();
        runner.save(saved);
        assertThat(saved.getString("CurrentPhase").orElseThrow()).isEqualTo(Phase.CLEARING.name());
        assertThat(saved.getInt("FrameBuildIndex").orElseThrow()).isZero();
        assertThat(saved.getInt("MiningX").orElseThrow()).isZero();
        assertThat(saved.getInt("MiningY").orElseThrow()).isZero();
        assertThat(saved.getInt("MiningZ").orElseThrow()).isZero();
        assertThat(saved.getFloat("BreakProgress").orElseThrow()).isZero();
        assertThat(saved.getBoolean("MiningFinished").orElseThrow()).isFalse();
        assertThat(runner.getCurrentTarget()).isNull();
        assertThat(runner.getCurrentBreakTime()).isEqualTo(-1f);
    }
}
