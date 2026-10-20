package com.logistics.forestry.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.logic.TreeView.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class TreeFellerTest {

    private static final BlockPos ORIGIN = BlockPos.ZERO;

    private static final TreeFeller.Limits LIMITS = TreeFeller.Limits.around(ORIGIN, 8);

    // --- tree shapes ---

    @Test
    void smallOak_takesTrunkAndLeafBlob() {
        FakeView view = smallOak(new FakeView(), 0, 0);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        assertSkipped(TreeFeller.plan(view, ORIGIN, LIMITS), FellPlan.Reason.NO_CANOPY);
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

        assertSkipped(TreeFeller.plan(view, ORIGIN, LIMITS), FellPlan.Reason.NO_CANOPY);
    }

    @Test
    void persistentLeavesAroundLogs_isNoCanopy() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 4, Kind.WOOD);
        // player-placed leaves are classified OTHER by the world adapter
        view.layer(-2, 2, 3, -2, 2, true, Kind.OTHER);
        view.layer(-1, 1, 5, -1, 1, false, Kind.OTHER);

        assertSkipped(TreeFeller.plan(view, ORIGIN, LIMITS), FellPlan.Reason.NO_CANOPY);
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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertThat(plan.isFound()).isTrue();
        List<BlockPos> wood = plan.blocks().stream()
                .filter(pos -> view.blocks.get(pos) == Kind.WOOD)
                .toList();
        assertThat(wood).containsExactlyInAnyOrderElementsOf(columnPositions(0, 0, 0, 4));
        assertThat(plan.blocks()).doesNotContain(fenceLow, fenceHigh);
    }

    @Test
    void touchingCanopies_eachTreeTakesOnlyTheLeavesNearestItsOwnWood() {
        FakeView view = smallOak(smallOak(new FakeView(), 0, 0), 2, 0);
        Map<BlockPos, Integer> fromWest = view.stepsFrom(columnPositions(0, 0, 0, 4));
        Map<BlockPos, Integer> fromEast = view.stepsFrom(columnPositions(2, 0, 0, 4));

        FellPlan west = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertThat(west.isFound()).isTrue();
        for (BlockPos leaf : view.canopy()) {
            int w = fromWest.getOrDefault(leaf, Integer.MAX_VALUE);
            int e = fromEast.getOrDefault(leaf, Integer.MAX_VALUE);
            if (w < e && w <= TreeFeller.CANOPY_REACH) {
                assertThat(west.blocks()).as("%s nearest the west trunk", leaf).contains(leaf);
            } else if (e < w) {
                assertThat(west.blocks()).as("%s nearest the east trunk", leaf).doesNotContain(leaf);
            }
        }
        assertThat(west.blocks()).doesNotContainAnyElementsOf(columnPositions(2, 0, 0, 4));

        west.blocks().forEach(view.blocks::remove);
        FellPlan east = TreeFeller.plan(view, new BlockPos(2, 0, 0), LIMITS);

        assertThat(east.isFound()).as("the east tree keeps a canopy: %s", east.reason()).isTrue();
        assertFoundExactly(east, view.all());
    }

    @Test
    void equidistantLeaf_goesWithEitherTree_neverStrandingTheOther() {
        BlockPos between = new BlockPos(1, 3, 0);
        for (boolean westFirst : List.of(true, false)) {
            FakeView view = smallOak(smallOak(new FakeView(), 0, 0), 2, 0);
                BlockPos first = westFirst ? ORIGIN : new BlockPos(2, 0, 0);
            BlockPos second = westFirst ? new BlockPos(2, 0, 0) : ORIGIN;

            FellPlan felled = TreeFeller.plan(view, first, LIMITS);
            assertThat(felled.blocks()).as("the leaf as near both trunks").contains(between);
            felled.blocks().forEach(view.blocks::remove);
                FellPlan other = TreeFeller.plan(view, second, LIMITS);

            assertThat(other.isFound()).as("the other tree keeps a canopy").isTrue();
            assertFoundExactly(other, view.all());
        }
    }

    @Test
    void persistentLeavesInTheCanopy_areNeverTaken() {
        FakeView view = smallOak(new FakeView(), 0, 0);
        // player-placed leaves are classified OTHER by the world adapter
        List<BlockPos> placed = List.of(new BlockPos(1, 3, 0), new BlockPos(0, 5, 0), new BlockPos(-2, 2, 1));
        placed.forEach(pos -> view.set(pos, Kind.OTHER));

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks()).doesNotContainAnyElementsOf(placed);
    }

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks()).doesNotContain(hive, behindHive);
        assertThat(view.queried).doesNotContain(behindHive, new BlockPos(3, 1, 0));
    }

    @Test
    void unloadedRegion_truncatesTheFillWithoutReadingPastIt() {
        FakeView view = smallOak(new FakeView(), 0, 0);
        view.unloaded = pos -> pos.getX() >= 2;

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan atCap = TreeFeller.plan(view, ORIGIN, LIMITS.withCap(size));
        FellPlan overCap = TreeFeller.plan(view, ORIGIN, LIMITS.withCap(size - 1));

        assertThat(atCap.isFound()).isTrue();
        assertThat(atCap.blocks()).hasSize(size);
        assertSkipped(overCap, FellPlan.Reason.CAP_EXCEEDED);
    }

    @Test
    void cap_stopsTheWoodFillEarly() {
        FakeView view = new FakeView();
        view.column(0, 0, 0, 20, Kind.WOOD);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS.withCap(10));

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, TreeFeller.Limits.around(ORIGIN, 2).withHeight(3, 5));

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

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertThat(plan.blocks())
                .contains(new BlockPos(TreeFeller.CANOPY_REACH, 3, 0))
                .doesNotContain(new BlockPos(TreeFeller.CANOPY_REACH + 1, 3, 0), new BlockPos(8, 3, 0))
                .doesNotContain(diagonalOnly);
    }

    @Test
    void limits_aroundAPlot() {
        assertThat(LIMITS).isEqualTo(new TreeFeller.Limits(
                new Columns(-8, -8, 8, 8), new Columns(-8, -8, 8, 8), Columns.EMPTY, 3, 32, 1024));
        assertThatThrownBy(() -> LIMITS.withCap(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LIMITS.withHeight(-1, 32)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeFeller.Limits.around(ORIGIN, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limits_withinARegion() {
        Columns bounds = Columns.square(ORIGIN, 11);
        Columns courtyard = Columns.square(ORIGIN, 7);

        TreeFeller.Limits limits = TreeFeller.Limits.within(bounds, courtyard);

        assertThat(limits.wood()).isEqualTo(bounds);
        assertThat(limits.canopy()).isEqualTo(new Columns(-8, -8, 8, 8));
        assertThat(limits.keepOut()).isEqualTo(courtyard);
        assertThat(limits.below()).isEqualTo(3);
        assertThat(limits.above()).isEqualTo(32);
        assertThat(limits.cap()).isEqualTo(1024);
    }

    // --- farm region bounds (a 7×7 courtyard, ring out to 11×11, centred on the origin) ---

    private static final TreeFeller.Limits REGION =
            TreeFeller.Limits.within(Columns.square(ORIGIN, 11), Columns.square(ORIGIN, 7));

    /** First lane of the ring, east of the courtyard. */
    private static final BlockPos RING_PLOT = new BlockPos(4, 0, 0);

    @Test
    void region_canopyOverTheCourtyardThatIsNotLeavesIsNotPlanned() {
        FakeView view = smallOak(new FakeView(), RING_PLOT.getX(), RING_PLOT.getZ());
        view.blocks.keySet().stream().filter(REGION.keepOut()::contains).forEach(view.notLeaves::add);

        FellPlan plan = TreeFeller.plan(view, RING_PLOT, REGION);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks())
                .noneMatch(REGION.keepOut()::contains)
                .doesNotContain(new BlockPos(3, 3, 0), new BlockPos(2, 3, 1), new BlockPos(3, 5, 0))
                .as("canopy past the outer edge, within the margin")
                .contains(new BlockPos(6, 3, 0), new BlockPos(5, 5, 0));
    }

    @Test
    void region_ownLeavesOverTheCourtyardAreTaken_nothingElseThere() {
        FakeView view = smallOak(new FakeView(), 5, 0);
        BlockPos courtyardVine = new BlockPos(3, 3, 1);
        BlockPos courtyardLog = new BlockPos(2, 4, 0);
        view.set(courtyardLog, Kind.WOOD);
        view.notLeaves.add(courtyardVine);

        FellPlan plan = TreeFeller.plan(view, new BlockPos(5, 0, 0), REGION);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks())
                .as("the tree's own leaves hanging into the courtyard")
                .contains(new BlockPos(3, 3, 0), new BlockPos(3, 2, -1))
                .doesNotContain(courtyardVine, courtyardLog);
        assertThat(plan.blocks().stream().filter(REGION.keepOut()::contains))
                .allMatch(pos -> view.blocks.get(pos) == Kind.CANOPY);
    }

    @Test
    void region_woodOutsideTheOuterSquareIsNotPlanned() {
        FakeView view = new FakeView();
        view.column(4, 0, 0, 3, Kind.WOOD);
        for (int x = 5; x <= 8; x++) {
            view.set(new BlockPos(x, 3, 0), Kind.WOOD);
        }
        view.layer(3, 5, 4, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, RING_PLOT, REGION);

        assertThat(plan.isFound()).isTrue();
        assertThat(plan.blocks())
                .contains(new BlockPos(5, 3, 0), new BlockPos(5, 4, 0))
                .doesNotContain(new BlockPos(6, 3, 0), new BlockPos(7, 3, 0), new BlockPos(8, 3, 0));
    }

    @Test
    void region_canopyStopsAtTheMargin() {
        FakeView view = new FakeView();
        view.column(5, 0, 0, 3, Kind.WOOD);
        for (int x = 6; x <= 10; x++) {
            view.set(new BlockPos(x, 3, 0), Kind.CANOPY);
        }

        FellPlan plan = TreeFeller.plan(view, new BlockPos(5, 0, 0), REGION);

        assertThat(plan.blocks())
                .contains(new BlockPos(8, 3, 0))
                .doesNotContain(new BlockPos(9, 3, 0), new BlockPos(10, 3, 0));
    }

    @Test
    void region_treeRootedInTheCourtyardIsNoTree() {
        FakeView view = smallOak(new FakeView(), 2, 0);

        assertSkipped(TreeFeller.plan(view, new BlockPos(2, 0, 0), REGION), FellPlan.Reason.NO_TREE);
    }

    @Test
    void region_neverWalksThroughTheCourtyard() {
        FakeView view = new FakeView();
        // A trunk on the west lane joined to one on the east lane only through the courtyard.
        view.column(-4, 0, 0, 3, Kind.WOOD);
        for (int x = -3; x <= 3; x++) {
            view.set(new BlockPos(x, 3, 0), Kind.WOOD);
        }
        view.column(4, 0, 0, 3, Kind.WOOD);
        view.layer(3, 5, 4, -1, 1, false, Kind.CANOPY);
        view.layer(-5, -3, 4, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, RING_PLOT, REGION);

        assertThat(plan.blocks())
                .contains(new BlockPos(4, 3, 0))
                .noneMatch(pos -> pos.getX() < 0);
    }

    // --- seeding ---

    @Test
    void noWoodAtOrWithinFourAbovePlot_isNoTree() {
        FakeView view = new FakeView();
        view.column(0, 0, 5, 9, Kind.WOOD);
        view.layer(-1, 1, 9, -1, 1, false, Kind.CANOPY);

        assertSkipped(TreeFeller.plan(view, ORIGIN, LIMITS), FellPlan.Reason.NO_TREE);
        assertSkipped(TreeFeller.plan(new FakeView(), ORIGIN, LIMITS), FellPlan.Reason.NO_TREE);
    }

    @Test
    void woodFourAbovePlot_isFound() {
        FakeView view = new FakeView();
        view.column(0, 0, 4, 8, Kind.WOOD);
        view.layer(-1, 1, 8, -1, 1, false, Kind.CANOPY);

        FellPlan plan = TreeFeller.plan(view, ORIGIN, LIMITS);

        assertFoundExactly(plan, view.all());
    }

    // --- ordering ---

    @Test
    void order_isTopDownCanopyFirstThenXThenZ() {
        FakeView view = smallOak(new FakeView(), 0, 0);

        List<BlockPos> blocks = TreeFeller.plan(view, ORIGIN, LIMITS).blocks();

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

        List<BlockPos> first = TreeFeller.plan(forward, ORIGIN, LIMITS).blocks();
        List<BlockPos> second = TreeFeller.plan(reversed, ORIGIN, LIMITS).blocks();
        List<BlockPos> third = TreeFeller.plan(forward, ORIGIN, LIMITS).blocks();

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
        /** CANOPY positions that are not leaves, e.g. vines. */
        final Set<BlockPos> notLeaves = new HashSet<>();
        final Set<BlockPos> queried = new HashSet<>();
        Predicate<BlockPos> unloaded = pos -> false;

        @Override
        public boolean isLeaves(BlockPos pos) {
            return blocks.get(pos) == Kind.CANOPY && !notLeaves.contains(pos);
        }

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

        /** Face steps from {@code wood} to each CANOPY position through CANOPY only. */
        Map<BlockPos, Integer> stepsFrom(List<BlockPos> wood) {
            Map<BlockPos, Integer> steps = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            wood.forEach(pos -> {
                steps.put(pos, 0);
                queue.add(pos);
            });
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (Direction side : Direction.values()) {
                    BlockPos beside = pos.relative(side);
                    if (blocks.get(beside) == Kind.CANOPY && !steps.containsKey(beside)) {
                        steps.put(beside, steps.get(pos) + 1);
                        queue.add(beside);
                    }
                }
            }
            wood.forEach(steps::remove);
            return steps;
        }

        Set<BlockPos> canopy() {
            Set<BlockPos> canopy = new HashSet<>();
            blocks.forEach((pos, kind) -> {
                if (kind == Kind.CANOPY) {
                    canopy.add(pos);
                }
            });
            return canopy;
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
