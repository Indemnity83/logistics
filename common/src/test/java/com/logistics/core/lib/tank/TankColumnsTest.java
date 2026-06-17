package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the cross-mod column walker via its in-memory {@link TankColumns.CellSource} seam, so the
 * walking logic ({@code bottomOf} / {@code column} / {@code isColumnBottom}) is tested without a live world.
 */
@DisplayName("TankColumns")
class TankColumnsTest extends MinecraftTestEnvironment {

    /** Generous loop-guard bound for the in-memory tests; larger than any stack built below. */
    private static final int BOUND = 512;

    /** Minimal cell carrying only what the walker reads: its capacity and whether it joins its column. */
    private static final class FakeCell implements TankCell {
        private final boolean joins;

        FakeCell(boolean joins) {
            this.joins = joins;
        }

        @Override public IFluidKey fluid() { return SimpleFluidKey.BLANK; }
        @Override public long amount() { return 0; }
        @Override public long capacity() { return 100; }
        @Override public void setContents(IFluidKey fluid, long amount) {}
        @Override public boolean joinsColumn() { return joins; }
    }

    /** In-memory world: a position holds a cell iff it was placed in the map. */
    private static final class FakeWorld {
        private final Map<BlockPos, TankCell> cells = new HashMap<>();

        FakeWorld put(int y, TankCell cell) {
            cells.put(new BlockPos(0, y, 0), cell);
            return this;
        }

        TankColumns.CellSource source() {
            return cells::get;
        }
    }

    private static BlockPos at(int y) {
        return new BlockPos(0, y, 0);
    }

    @Test
    @DisplayName("bottomOf walks down a connected stack")
    void bottomOfWalksDown() {
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, new FakeCell(true))
                .put(2, new FakeCell(true))
                .source();

        assertThat(TankColumns.bottomOf(src, at(2), BOUND)).isEqualTo(at(0));
    }

    @Test
    @DisplayName("bottomOf stops at a gap below")
    void bottomOfStopsAtGap() {
        TankColumns.CellSource src = new FakeWorld()
                .put(1, new FakeCell(true))
                .put(2, new FakeCell(true))
                .source(); // nothing at y=0

        assertThat(TankColumns.bottomOf(src, at(2), BOUND)).isEqualTo(at(1));
    }

    @Test
    @DisplayName("bottomOf treats an isolated cell as its own bottom")
    void bottomOfIsolated() {
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, new FakeCell(false)) // isolated: does not join the cell below
                .source();

        assertThat(TankColumns.bottomOf(src, at(1), BOUND)).isEqualTo(at(1));
    }

    @Test
    @DisplayName("bottomOf does not descend through an isolated neighbour below")
    void bottomOfStopsAtIsolatedBelow() {
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, new FakeCell(false)) // isolated cell breaks the connection
                .put(2, new FakeCell(true))
                .source();

        assertThat(TankColumns.bottomOf(src, at(2), BOUND)).isEqualTo(at(2));
    }

    @Test
    @DisplayName("bottomOf on an empty position returns that position")
    void bottomOfEmpty() {
        assertThat(TankColumns.bottomOf(new FakeWorld().source(), at(5), BOUND)).isEqualTo(at(5));
    }

    @Test
    @DisplayName("column returns connected cells bottom-to-top")
    void columnBottomToTop() {
        FakeCell c0 = new FakeCell(true);
        FakeCell c1 = new FakeCell(true);
        FakeCell c2 = new FakeCell(true);
        TankColumns.CellSource src = new FakeWorld().put(0, c0).put(1, c1).put(2, c2).source();

        // Starting anywhere in the stack yields the same ordered column.
        assertThat(TankColumns.column(src, at(1), BOUND)).containsExactly(c0, c1, c2);
        assertThat(TankColumns.column(src, at(0), BOUND)).containsExactly(c0, c1, c2);
    }

    @Test
    @DisplayName("column stops at a gap above")
    void columnStopsAtGapAbove() {
        FakeCell c0 = new FakeCell(true);
        FakeCell c1 = new FakeCell(true);
        TankColumns.CellSource src = new FakeWorld().put(0, c0).put(1, c1).source(); // nothing at y=2

        assertThat(TankColumns.column(src, at(0), BOUND)).containsExactly(c0, c1);
    }

    @Test
    @DisplayName("column of an isolated cell is just that cell")
    void columnIsolatedSingle() {
        FakeCell isolated = new FakeCell(false);
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, isolated)
                .source();

        assertThat(TankColumns.column(src, at(1), BOUND)).containsExactly(isolated);
    }

    @Test
    @DisplayName("column of an empty position is empty")
    void columnEmpty() {
        assertThat(TankColumns.column(new FakeWorld().source(), at(0), BOUND)).isEmpty();
    }

    @Test
    @DisplayName("isColumnBottom is true only for the lowest connected cell")
    void isColumnBottomLowestOnly() {
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, new FakeCell(true))
                .source();

        assertThat(TankColumns.isColumnBottom(src, at(0))).isTrue();
        assertThat(TankColumns.isColumnBottom(src, at(1))).isFalse();
    }

    @Test
    @DisplayName("an isolated cell is always its own bottom")
    void isColumnBottomIsolated() {
        TankColumns.CellSource src = new FakeWorld()
                .put(0, new FakeCell(true))
                .put(1, new FakeCell(false))
                .source();

        // Even with a joinable cell directly below, the isolated cell rebalances itself.
        assertThat(TankColumns.isColumnBottom(src, at(1))).isTrue();
    }

    @Test
    @DisplayName("isColumnBottom is false for an empty position")
    void isColumnBottomEmpty() {
        assertThat(TankColumns.isColumnBottom(new FakeWorld().source(), at(0))).isFalse();
    }

    @Test
    @DisplayName("a tall column is walked in one piece up to the world height")
    void wholeColumnUpToWorldHeight() {
        // A 300-tall stack — taller than the old fixed 256 cap. With a world-height bound it stays one column.
        FakeWorld world = new FakeWorld();
        for (int y = 0; y < 300; y++) {
            world.put(y, new FakeCell(true));
        }
        TankColumns.CellSource src = world.source();
        int worldHeight = 384; // overworld block height in 26.1

        assertThat(TankColumns.column(src, at(0), worldHeight)).hasSize(300);
        assertThat(TankColumns.bottomOf(src, at(299), worldHeight)).isEqualTo(at(0));
    }

    @Test
    @DisplayName("maxHeight bounds the walk as a defensive loop guard")
    void maxHeightCapsTheWalk() {
        FakeWorld world = new FakeWorld();
        for (int y = 0; y < 300; y++) {
            world.put(y, new FakeCell(true));
        }
        TankColumns.CellSource src = world.source();

        // With a tight cap the walk stops early rather than running unbounded over a pathological source.
        // Start well above the true floor (y=0) so a missing cap would overshoot: capped at 10 steps from
        // y=100 the bottom is y=90, not the real floor — which is what distinguishes capped from uncapped.
        assertThat(TankColumns.column(src, at(0), 10)).hasSize(10);
        assertThat(TankColumns.bottomOf(src, at(100), 10)).isEqualTo(at(90));
    }
}
