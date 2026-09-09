package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Energy already sunk into a block has to survive a save. The quarry only carries break progress
 * over when the block it is aimed at is the same one it was breaking, so the target has to persist
 * alongside the progress — a restored progress value with no target is discarded on the first tick.
 */
@DisplayName("Quarry break progress persistence")
class QuarryBreakProgressTest extends MinecraftTestEnvironment {

    private static final BlockPos TARGET = new BlockPos(12, 40, -7);

    /** A save written part-way through breaking the block at {@link #TARGET}. */
    private static CompoundTag midBreakSave() {
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", QuarryPhase.MINING.name());
        tag.putInt("MiningX", 3);
        tag.putInt("MiningY", 8);
        tag.putInt("MiningZ", 1);
        tag.putFloat("BreakProgress", 6000f);
        tag.putIntArray("CurrentTarget", new int[] {TARGET.getX(), TARGET.getY(), TARGET.getZ()});
        return tag;
    }

    @Test
    @DisplayName("a quarry saved part-way through a block reloads still aimed at that block")
    void breakTargetSurvivesSaveAndLoad() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        runner.load(midBreakSave());

        assertThat(runner.getBreakProgress()).isEqualTo(6000f);
        assertThat(runner.getCurrentTarget())
                .as("without the target the next break tick sees a different block and restarts it from zero")
                .isEqualTo(TARGET);
    }

    @Test
    @DisplayName("the in-flight target is written back out, so it survives repeated save/load cycles")
    void breakTargetIsWrittenBack() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        runner.load(midBreakSave());

        CompoundTag written = new CompoundTag();
        runner.save(written);

        assertThat(NbtCompat.getFloat(written, "BreakProgress", -1f)).isEqualTo(6000f);
        assertThat(NbtCompat.getIntArray(written, "CurrentTarget", new int[0]))
                .containsExactly(TARGET.getX(), TARGET.getY(), TARGET.getZ());
    }

    @Test
    @DisplayName("the break cost is not carried in the save, so it is recomputed from the block that is really there")
    void breakCostIsNotRestoredFromNbt() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        runner.load(midBreakSave());

        assertThat(runner.getCurrentBreakTime())
                .as("a block can be swapped while the chunk is unloaded; its cost must come from the live state")
                .isNegative();
    }

    @Test
    @DisplayName("a save with no in-flight target loads with no target and no crash")
    void olderSaveWithoutATargetLoadsCleanly() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", QuarryPhase.MINING.name());
        tag.putFloat("BreakProgress", 6000f);
        runner.load(tag);

        assertThat(runner.getCurrentTarget()).isNull();
        assertThat(runner.getBreakProgress()).isEqualTo(6000f);

        CompoundTag written = new CompoundTag();
        runner.save(written);
        assertThat(NbtCompat.getIntArray(written, "CurrentTarget", new int[0]))
                .as("nothing in flight, so nothing to write")
                .isEmpty();
    }
}
