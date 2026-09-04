package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsAutomation;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

/**
 * A frame block broken out from under a running quarry has to be noticed, rebuilt at the normal
 * frame-build cost, and mining has to pick up exactly where it stopped.
 */
@DisplayName("Quarry frame repair")
class QuarryFrameRepairTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A frame laid out as a straight run of positions, so an index maps to a known block. */
    private static final int FRAME_SIZE = 20;

    private final Map<BlockPos, BlockState> world = new HashMap<>();

    private static BlockPos posAt(int index) {
        return new BlockPos(index, 64, 0);
    }

    private final IntFunction<@Nullable BlockPos> positions =
            index -> index < FRAME_SIZE ? posAt(index) : null;

    private final Function<BlockPos, BlockState> states =
            pos -> world.getOrDefault(pos, Blocks.AIR.defaultBlockState());

    private void buildWholeFrame() {
        for (int i = 0; i < FRAME_SIZE; i++) {
            world.put(posAt(i), LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME.defaultBlockState());
        }
    }

    // ==================== what counts as a gap ====================

    @Test
    @DisplayName("a standing frame block is not a gap")
    void standingFrameIsNotAGap() {
        assertThat(QuarryPhaseRunner.isGap(
                        LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME.defaultBlockState()))
                .isFalse();
    }

    @Test
    @DisplayName("air where a frame block belongs is a gap")
    void airIsAGap() {
        assertThat(QuarryPhaseRunner.isGap(Blocks.AIR.defaultBlockState())).isTrue();
    }

    @Test
    @DisplayName("a replaceable block is a gap")
    void replaceableIsAGap() {
        assertThat(QuarryPhaseRunner.isGap(Blocks.SHORT_GRASS.defaultBlockState()))
                .as("grass can be replaced, so the quarry should rebuild through it")
                .isTrue();
    }

    @Test
    @DisplayName("a player-placed solid block is not a gap, so repair can never wedge on it")
    void solidBlockIsNotAGap() {
        assertThat(QuarryPhaseRunner.isGap(Blocks.STONE.defaultBlockState()))
                .as("stone cannot be replaced; calling it a gap would loop the repair phase forever")
                .isFalse();
    }

    // ==================== finding the gap ====================

    @Test
    @DisplayName("an intact frame reports no gap")
    void intactFrameHasNoGap() {
        buildWholeFrame();
        assertThat(QuarryPhaseRunner.nextGapIndex(positions, states, 0)).isEqualTo(-1);
    }

    @Test
    @DisplayName("a broken block is found at its own index")
    void brokenBlockIsFound() {
        buildWholeFrame();
        world.put(posAt(13), Blocks.AIR.defaultBlockState());
        assertThat(QuarryPhaseRunner.nextGapIndex(positions, states, 0)).isEqualTo(13);
    }

    @Test
    @DisplayName("repair walks every gap, then reports the frame whole")
    void repairWalksEveryGap() {
        buildWholeFrame();
        world.put(posAt(4), Blocks.AIR.defaultBlockState());
        world.put(posAt(11), Blocks.AIR.defaultBlockState());

        int first = QuarryPhaseRunner.nextGapIndex(positions, states, 0);
        assertThat(first).isEqualTo(4);
        world.put(posAt(first), LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME.defaultBlockState());

        int second = QuarryPhaseRunner.nextGapIndex(positions, states, first + 1);
        assertThat(second).isEqualTo(11);
        world.put(posAt(second), LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME.defaultBlockState());

        assertThat(QuarryPhaseRunner.nextGapIndex(positions, states, 0))
                .as("frame is whole again")
                .isEqualTo(-1);
    }

    // ==================== the rolling sweep ====================

    @Test
    @DisplayName("the sweep covers the whole frame across ticks and finds a late gap")
    void sweepCoversWholeFrameAcrossTicks() {
        buildWholeFrame();
        world.put(posAt(FRAME_SIZE - 1), Blocks.AIR.defaultBlockState());

        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        boolean found = false;
        // The sweep checks a slice per tick; the last position must still be reached.
        for (int tick = 0; tick < FRAME_SIZE && !found; tick++) {
            found = runner.frameHasGap(positions, states);
        }
        assertThat(found)
                .as("a gap at the far end of the frame must still be noticed by the rolling sweep")
                .isTrue();
    }

    @Test
    @DisplayName("an intact frame never reports a gap however long the sweep runs")
    void intactFrameNeverTripsTheSweep() {
        buildWholeFrame();
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        for (int tick = 0; tick < FRAME_SIZE * 3; tick++) {
            assertThat(runner.frameHasGap(positions, states)).isFalse();
        }
    }

    @Test
    @DisplayName("a frame that resolves no positions is not policed")
    void unresolvableFrameIsNotPoliced() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        assertThat(runner.frameHasGap(index -> null, states))
                .as("no frame to check means no repair, not an infinite sweep")
                .isFalse();
    }


    // ==================== clearance band ====================

    private static final QuarryFrameRect RECT = new QuarryFrameRect(10, 20, 12, 23, 64, 66);

    @Test
    @DisplayName("the clearance band covers the whole rectangle from bottom to top")
    void clearanceBandCoversTheRectangle() {
        int width = RECT.width();
        int depth = RECT.depth();
        int height = RECT.topY() - RECT.bottomY() + 1;

        Set<BlockPos> seen = new HashSet<>();
        for (int i = 0; i < width * depth * height; i++) {
            BlockPos pos = QuarryPhaseRunner.clearancePositionAt(RECT, i);
            assertThat(pos).as("index " + i + " must resolve").isNotNull();
            assertThat(pos.getX()).isBetween(RECT.startX(), RECT.endX());
            assertThat(pos.getZ()).isBetween(RECT.startZ(), RECT.endZ());
            assertThat(pos.getY()).isBetween(RECT.bottomY(), RECT.topY());
            seen.add(pos);
        }
        assertThat(seen)
                .as("every position in the band is visited exactly once")
                .hasSize(width * depth * height);
    }

    @Test
    @DisplayName("the clearance band ends, so the sweep terminates")
    void clearanceBandTerminates() {
        int total = RECT.width() * RECT.depth() * (RECT.topY() - RECT.bottomY() + 1);
        assertThat(QuarryPhaseRunner.clearancePositionAt(RECT, total)).isNull();
        assertThat(QuarryPhaseRunner.clearancePositionAt(RECT, -1)).isNull();
    }

    @Test
    @DisplayName("a block dropped into the band is an intrusion")
    void droppedBlockIsAnIntrusion() {
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(
                        EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.COBBLESTONE.defaultBlockState()))
                .as("cobble in a frame slot is exactly the case this phase exists for")
                .isTrue();
    }

    @Test
    @DisplayName("the quarry's own frame is never an intrusion")
    void frameBlockIsNotAnIntrusion() {
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO,
                        LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME.defaultBlockState()))
                .isFalse();
    }

    @Test
    @DisplayName("air is not an intrusion")
    void airIsNotAnIntrusion() {
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.AIR.defaultBlockState()))
                .isFalse();
    }

    @Test
    @DisplayName("blocks the quarry could never mine are not intrusions, so they cannot wedge the phase")
    void unmineableBlocksAreNotIntrusions() {
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.LAVA.defaultBlockState()))
                .as("lava is left alone everywhere else in the quarry; clearing must agree")
                .isFalse();
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.WATER.defaultBlockState()))
                .isFalse();
        assertThat(QuarryPhaseRunner.isClearanceIntrusion(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.BEDROCK.defaultBlockState()))
                .as("bedrock has destroy speed -1; trying to clear it would loop forever")
                .isFalse();
    }

    @Test
    @DisplayName("a clearance cursor round-trips through NBT")
    void clearanceCursorSurvivesSaveAndLoad() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", QuarryPhase.MAINTAINING_CLEARANCE.name());
        tag.putInt("ClearanceIndex", 91);
        tag.putInt("MiningY", 6);
        runner.load(tag);

        assertThat(runner.getPhase()).isEqualTo(QuarryPhase.MAINTAINING_CLEARANCE);
        assertThat(runner.getMiningY()).as("mining cursor is untouched by a clearance detour").isEqualTo(6);

        CompoundTag written = new CompoundTag();
        runner.save(written);
        assertThat(written.getInt("ClearanceIndex")).hasValue(91);
    }

    // ==================== persistence ====================

    @Test
    @DisplayName("a quarry saved mid-repair reloads mid-repair with its mining cursor intact")
    void repairSurvivesSaveAndLoad() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", QuarryPhase.REPAIRING_FRAME.name());
        tag.putInt("FrameRepairIndex", 7);
        tag.putInt("FrameScanIndex", 3);
        tag.putInt("MiningX", 5);
        tag.putInt("MiningY", 9);
        tag.putInt("MiningZ", 2);
        runner.load(tag);

        assertThat(runner.getPhase()).isEqualTo(QuarryPhase.REPAIRING_FRAME);
        assertThat(runner.getMiningX()).isEqualTo(5);
        assertThat(runner.getMiningY()).isEqualTo(9);
        assertThat(runner.getMiningZ()).isEqualTo(2);

        CompoundTag written = new CompoundTag();
        runner.save(written);
        assertThat(written.getString("CurrentPhase")).hasValue(QuarryPhase.REPAIRING_FRAME.name());
        assertThat(written.getInt("FrameRepairIndex")).hasValue(7);
        assertThat(written.getInt("FrameScanIndex")).hasValue(3);
    }

    @Test
    @DisplayName("a pre-repair save loads with the new cursors zeroed")
    void oldSaveLoadsCleanly() {
        QuarryPhaseRunner runner = new QuarryPhaseRunner();
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", QuarryPhase.MINING.name());
        tag.putInt("MiningY", 4);
        runner.load(tag);

        assertThat(runner.getPhase()).isEqualTo(QuarryPhase.MINING);
        assertThat(runner.getMiningY()).isEqualTo(4);

        CompoundTag written = new CompoundTag();
        runner.save(written);
        assertThat(written.getInt("FrameRepairIndex")).hasValue(0);
        assertThat(written.getInt("FrameScanIndex")).hasValue(0);
    }
}
