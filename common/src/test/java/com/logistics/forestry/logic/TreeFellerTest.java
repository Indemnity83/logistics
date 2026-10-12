package com.logistics.forestry.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.forestry.logic.TreeView.Kind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TreeFellerTest {

    private static final BlockPos ORIGIN = BlockPos.ZERO;

    // --- tree shapes ---

    @Test
    void smallOak_takesTrunkAndLeafBlob() {
        FakeView view = smallOak(new FakeView(), 0, 0);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
    }

    @Test
    void birch_takesTallTrunkAndLeaves() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 6, Kind.WOOD);
        view.layer(-2, 2, 3, -2, 2, true, Kind.CANOPY);
        view.layer(-2, 2, 4, -2, 2, true, Kind.CANOPY);
        view.layer(-1, 1, 5, -1, 1, false, Kind.CANOPY);
        view.layer(-1, 1, 6, -1, 1, false, Kind.CANOPY);
        view.plus(0, 7, 0, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
    }

    @Test
    void darkOak_takesTwoByTwoTrunkAndCanopy() {
        FakeView view = new FakeView();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                view.column(x, z, 0, 5, Kind.WOOD);
            }
        }
        view.layer(-2, 3, 4, -2, 3, true, Kind.CANOPY);
        view.layer(-2, 3, 5, -2, 3, true, Kind.CANOPY);
        view.layer(-1, 2, 6, -1, 2, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
    }

    @Test
    void megaJungle_followsDiagonalBranches() {
        FakeView view = new FakeView();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                view.column(x, z, 0, 14, Kind.WOOD);
            }
        }
        // branches that touch the trunk and each other only at corners
        List<BlockPos> branches = List.of(
                new BlockPos(2, 11, 2), new BlockPos(3, 12, 3), new BlockPos(4, 13, 4),
                new BlockPos(-1, 10, -1), new BlockPos(-2, 11, -2));
        branches.forEach(pos -> view.set(pos, Kind.WOOD));
        view.layer(3, 5, 14, 3, 5, false, Kind.CANOPY);
        view.set(new BlockPos(-2, 12, -2), Kind.CANOPY);
        view.layer(-2, 3, 15, -2, 3, true, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
        assertThat(plan.blocks()).containsAll(branches);
    }

    @Test
    void acacia_followsDiagonalTrunkAndFork() {
        FakeView view = new FakeView();
        List<BlockPos> trunk = List.of(
                new BlockPos(0, 0, 0), new BlockPos(0, 1, 0), new BlockPos(1, 2, 0),
                new BlockPos(2, 3, 0), new BlockPos(3, 4, 0), new BlockPos(3, 5, 0),
                new BlockPos(-1, 2, 1), new BlockPos(-2, 3, 1));
        trunk.forEach(pos -> view.set(pos, Kind.WOOD));
        view.layer(1, 5, 6, -2, 2, true, Kind.CANOPY);
        view.layer(-3, -1, 4, 0, 2, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
    }

    @Test
    void mangrove_seedsAboveThePlotAndTakesRootsDownToTheBound() {
        FakeView view = new FakeView();
        view.column(0, 0, 2, 7, Kind.WOOD);
        List<BlockPos> roots = List.of(
                new BlockPos(1, 1, 0), new BlockPos(2, 0, 0), new BlockPos(3, -1, 0),
                new BlockPos(-1, 1, 0), new BlockPos(-2, 0, 0), new BlockPos(-3, -1, 0),
                new BlockPos(0, 1, 1), new BlockPos(0, 0, 2), new BlockPos(0, -1, 3));
        roots.forEach(pos -> view.set(pos, Kind.WOOD));
        BlockPos tooDeep = new BlockPos(4, -2, 0);
        view.set(tooDeep, Kind.WOOD);
        view.layer(-2, 2, 7, -2, 2, true, Kind.CANOPY);
        view.layer(-2, 2, 8, -2, 2, true, Kind.CANOPY);
        BlockPos propagule = new BlockPos(2, 6, 0);
        view.set(propagule, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        Set<BlockPos> expected = new HashSet<>(view.all());
        expected.remove(tooDeep);
        assertFoundExactly(plan, expected);
        assertThat(plan.blocks()).containsAll(roots).contains(propagule);
    }

    // --- player builds ---

    @Test
    void bareLogColumn_isNoCanopy() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 5, Kind.WOOD);

        assertSkipped(TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT), FellPlan.Reason.NO_CANOPY);
    }

    @Test
    void logHouse_isNoCanopy() {
        FakeView view = new FakeView();
        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    if (x == 0 || x == 4 || z == 0 || z == 4) {
                        view.set(new BlockPos(x, y, z), Kind.WOOD);
                    }
                }
            }
        }

        assertSkipped(TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT), FellPlan.Reason.NO_CANOPY);
    }

    @Test
    void persistentLeavesAroundLogs_isNoCanopy() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 4, Kind.WOOD);
        // player-placed leaves are classified OTHER by the world adapter
        view.layer(-2, 2, 3, -2, 2, true, Kind.OTHER);
        view.layer(-1, 1, 5, -1, 1, false, Kind.OTHER);

        assertSkipped(TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT), FellPlan.Reason.NO_CANOPY);
    }

    @Test
    void touchingCanopies_neverStepIntoAnotherTrunkOrLogs() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 4, Kind.WOOD);
        view.column(5, 0, 0, 4, Kind.WOOD);
        for (int y = 3; y <= 5; y++) {
            view.layer(-2, 2, y, -2, 2, false, Kind.CANOPY);
            view.layer(3, 7, y, -2, 2, false, Kind.CANOPY);
        }
        // logs resting on the canopy but not touching the seeded trunk
        BlockPos fenceLow = new BlockPos(2, 6, 0);
        BlockPos fenceHigh = new BlockPos(2, 7, 0);
        view.set(fenceLow, Kind.WOOD);
        view.set(fenceHigh, Kind.WOOD);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertThat(plan.isFound()).isTrue();
        List<BlockPos> wood = plan.blocks().stream()
                .filter(pos -> view.blocks.get(pos) == Kind.WOOD)
                .toList();
        assertThat(wood).containsExactlyInAnyOrderElementsOf(columnPositions(0, 0, 0, 4));
        assertThat(plan.blocks()).doesNotContain(fenceLow, fenceHigh);
    }

    // --- never touch, unloaded ---

    @Test
    void beehiveOnTrunk_isExcludedAndNotTraversed() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 4, Kind.WOOD);
        view.layer(-2, 2, 3, -2, 2, true, Kind.CANOPY);
        view.layer(-2, 2, 4, -2, 2, true, Kind.CANOPY);
        view.layer(-1, 1, 5, -1, 1, false, Kind.CANOPY);
        BlockPos hive = new BlockPos(1, 1, 0);
        BlockPos behindHive = new BlockPos(2, 1, 0);
        view.set(hive, Kind.NEVER_TOUCH);
        view.set(behindHive, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks()).doesNotContain(hive, behindHive);
        assertThat(view.queried).doesNotContain(behindHive, new BlockPos(3, 1, 0));
    }

    @Test
    void unloadedRegion_truncatesTheFillWithoutReadingPastIt() {
        FakeView view = smallOak(new FakeView(), 0, 0);
        view.unloaded = pos -> pos.getX() >= 2;

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks()).allMatch(pos -> pos.getX() < 2);
        assertThat(plan.blocks()).contains(new BlockPos(1, 3, 0), new BlockPos(-2, 3, 0));
        assertThat(view.queried).allMatch(pos -> pos.getX() <= 2);
    }

    // --- limits ---

    @Test
    void cap_isInclusive() {
        FakeView view = smallOak(new FakeView(), 0, 0);
        int size = view.all().size();

        FellPlan atCap = TreeFeller.plan(view, ORIGIN, new TreeFeller.Limits(8, 3, 32, size));
        FellPlan overCap = TreeFeller.plan(view, ORIGIN, new TreeFeller.Limits(8, 3, 32, size - 1));

        assertThat(atCap.isFound()).isTrue();
        assertThat(atCap.blocks()).hasSize(size);
        assertSkipped(overCap, FellPlan.Reason.CAP_EXCEEDED);
    }

    @Test
    void cap_stopsTheWoodFillEarly() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 20, Kind.WOOD);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, new TreeFeller.Limits(8, 3, 32, 10));

        assertSkipped(plan, FellPlan.Reason.CAP_EXCEEDED);
        assertThat(view.queried).doesNotContain(new BlockPos(0, 11, 0));
    }

    @Test
    void horizontalBound_excludesFarBranch() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 3, Kind.WOOD);
        for (int x = 1; x <= 10; x++) {
            view.set(new BlockPos(x, 3, 0), Kind.WOOD);
        }
        view.layer(-1, 1, 4, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks()).contains(new BlockPos(8, 3, 0));
        assertThat(plan.blocks()).doesNotContain(new BlockPos(9, 3, 0), new BlockPos(10, 3, 0));
    }

    @Test
    void verticalAndHorizontalBounds_excludeOutsideParts() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 10, Kind.WOOD);
        view.layer(-3, 3, 4, 0, 0, false, Kind.CANOPY);
        view.layer(-1, 1, 9, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, new TreeFeller.Limits(2, 3, 5, 1024));

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks())
                .allMatch(pos -> Math.abs(pos.getX()) <= 2 && Math.abs(pos.getZ()) <= 2)
                .allMatch(pos -> pos.getY() <= 5)
                .contains(new BlockPos(0, 5, 0), new BlockPos(2, 4, 0), new BlockPos(-2, 4, 0))
                .doesNotContain(new BlockPos(0, 6, 0), new BlockPos(3, 4, 0), new BlockPos(-3, 4, 0));
    }

    @Test
    void canopy_reachesSixFaceStepsFromWood() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 3, Kind.WOOD);
        for (int x = 1; x <= 8; x++) {
            view.set(new BlockPos(x, 3, 0), Kind.CANOPY);
        }
        BlockPos diagonalOnly = new BlockPos(-1, 4, 1);
        view.set(diagonalOnly, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertThat(plan.blocks())
                .contains(new BlockPos(TreeFeller.CANOPY_REACH, 3, 0))
                .doesNotContain(new BlockPos(TreeFeller.CANOPY_REACH + 1, 3, 0), new BlockPos(8, 3, 0))
                .doesNotContain(diagonalOnly);
    }

    @Test
    void defaultLimits() {
        assertThat(TreeFeller.Limits.DEFAULT).isEqualTo(new TreeFeller.Limits(8, 3, 32, 1024));
        assertThatThrownBy(() -> new TreeFeller.Limits(8, 3, 32, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreeFeller.Limits(-1, 3, 32, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- seeding ---

    @Test
    void noWoodAtOrWithinFourAbovePlot_isNoTree() {
        FakeView view = new FakeView();
        view.column(0, 0, 5, 9, Kind.WOOD);
        view.layer(-1, 1, 9, -1, 1, false, Kind.CANOPY);

        assertSkipped(TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT), FellPlan.Reason.NO_TREE);
        assertSkipped(TreeFeller.plan(new FakeView(), ORIGIN, TreeFeller.Limits.DEFAULT), FellPlan.Reason.NO_TREE);
    }

    @Test
    void woodFourAbovePlot_isFound() {
        FakeView view = new FakeView();
        view.column(0, 0, 4, 8, Kind.WOOD);
        view.layer(-1, 1, 8, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT);

        assertFoundExactly(plan, view.all());
    }

    // --- ordering ---

    @Test
    void order_isTopDownCanopyFirstThenXThenZ() {
        FakeView view = smallOak(new FakeView(), 0, 0);

        List<BlockPos> blocks = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.DEFAULT).blocks();

        for (int i = 1; i < blocks.size(); i++) {
            BlockPos prev = blocks.get(i - 1);
            BlockPos next = blocks.get(i);
            assertThat(next.getY()).isLessThanOrEqualTo(prev.getY());
            if (next.getY() == prev.getY()) {
                int prevRank = view.blocks.get(prev) == Kind.CANOPY ? 0 : 1;
                int nextRank = view.blocks.get(next) == Kind.CANOPY ? 0 : 1;
                assertThat(nextRank).isGreaterThanOrEqualTo(prevRank);
                if (nextRank == prevRank) {
                    assertThat(next.getX() > prev.getX()
                                    || (next.getX() == prev.getX() && next.getZ() > prev.getZ()))
                            .as("%s after %s", next, prev)
                            .isTrue();
                }
            }
        }
        // the trunk block at a leafy layer comes after that layer's leaves
        int trunk = blocks.indexOf(new BlockPos(0, 3, 0));
        int leaf = blocks.indexOf(new BlockPos(2, 3, 1));
        assertThat(leaf).isLessThan(trunk);
    }

    @Test
    void order_isIndependentOfViewIterationOrder() {
        FakeView forward = smallOak(new FakeView(), 0, 0);
        FakeView reversed = new FakeView();
        List<Map.Entry<BlockPos, Kind>> entries = new ArrayList<>(forward.blocks.entrySet());
        Collections.reverse(entries);
        entries.forEach(entry -> reversed.set(entry.getKey(), entry.getValue()));

        List<BlockPos> first = TreeFeller.plan(forward, ORIGIN, TreeFeller.Limits.DEFAULT).blocks();
        List<BlockPos> second = TreeFeller.plan(reversed, ORIGIN, TreeFeller.Limits.DEFAULT).blocks();
        List<BlockPos> third = TreeFeller.plan(forward, ORIGIN, TreeFeller.Limits.DEFAULT).blocks();

        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    // --- helpers ---

    private static FakeView smallOak(FakeView view, int x, int z) {
        view.column(x, z, 0, 4, Kind.WOOD);
        view.layer(x - 2, x + 2, 2, z - 2, z + 2, true, Kind.CANOPY);
        view.layer(x - 2, x + 2, 3, z - 2, z + 2, true, Kind.CANOPY);
        view.layer(x - 1, x + 1, 4, z - 1, z + 1, false, Kind.CANOPY);
        view.plus(x, 5, z, Kind.CANOPY);
        return view;
    }

    private static List<BlockPos> columnPositions(int x, int z, int y0, int y1) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = y0; y <= y1; y++) {
            positions.add(new BlockPos(x, y, z));
        }
        return positions;
    }

    private static void assertFoundExactly(FellPlan plan, Set<BlockPos> expected) {
        assertThat(plan.isFound()).as("plan skipped: %s", plan.reason()).isTrue();
        assertThat(plan.reason()).isNull();
        assertThat(plan.blocks()).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(expected);
    }

    private static void assertSkipped(FellPlan plan, FellPlan.Reason reason) {
        assertThat(plan.isFound()).isFalse();
        assertThat(plan.reason()).isEqualTo(reason);
        assertThat(plan.blocks()).isEmpty();
    }

    /** A world of OTHER except for the positions set; records every position read. */
    private static final class FakeView implements TreeView {
        final Map<BlockPos, Kind> blocks = new LinkedHashMap<>();
        final Set<BlockPos> queried = new HashSet<>();
        Predicate<BlockPos> unloaded = pos -> false;

        @Override
        public Kind kind(BlockPos pos) {
            queried.add(pos);
            if (unloaded.test(pos)) {
                return Kind.UNLOADED;
            }
            return blocks.getOrDefault(pos, Kind.OTHER);
        }

        void set(BlockPos pos, Kind kind) {
            blocks.put(pos, kind);
        }

        void column(int x, int z, int y0, int y1, Kind kind) {
            columnPositions(x, z, y0, y1).forEach(pos -> set(pos, kind));
        }

        /** Fills a horizontal rectangle, leaving positions already set alone. */
        void layer(int x0, int x1, int y, int z0, int z1, boolean cutCorners, Kind kind) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    if (cutCorners && corner) {
                        continue;
                    }
                    blocks.putIfAbsent(new BlockPos(x, y, z), kind);
                }
            }
        }

        void plus(int x, int y, int z, Kind kind) {
            blocks.putIfAbsent(new BlockPos(x, y, z), kind);
            blocks.putIfAbsent(new BlockPos(x + 1, y, z), kind);
            blocks.putIfAbsent(new BlockPos(x - 1, y, z), kind);
            blocks.putIfAbsent(new BlockPos(x, y, z + 1), kind);
            blocks.putIfAbsent(new BlockPos(x, y, z - 1), kind);
        }

        /** Every WOOD and CANOPY position. */
        Set<BlockPos> all() {
            Set<BlockPos> all = new HashSet<>();
            blocks.forEach((pos, kind) -> {
                if (kind == Kind.WOOD || kind == Kind.CANOPY) {
                    all.add(pos);
                }
            });
            return all;
        }
    }
}
