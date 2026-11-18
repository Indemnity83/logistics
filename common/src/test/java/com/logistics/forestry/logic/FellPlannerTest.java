package com.logistics.forestry.logic;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.logic.TreeView.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Resumable tree planner")
class FellPlannerTest {

    private static final BlockPos ORIGIN = BlockPos.ZERO;

    /** A view over a map of kinds, OTHER elsewhere, counting reads per position. */
    private static final class MapView implements TreeView {
        final Map<BlockPos, Kind> kinds = new HashMap<>();
        final Map<BlockPos, Integer> reads = new HashMap<>();
        int total;

        @Override
        public Kind kind(BlockPos pos) {
            total++;
            reads.merge(pos.immutable(), 1, Integer::sum);
            return kinds.getOrDefault(pos, Kind.OTHER);
        }
    }

    /** A random tree-ish blob: a crooked trunk with branches, leaves around its top, the odd stray block. */
    private static MapView randomTree(long seed) {
        Random random = new Random(seed);
        MapView view = new MapView();
        int height = 3 + random.nextInt(12);
        int x = 0;
        int z = 0;
        int base = random.nextInt(3); // the seed may sit up to two blocks above the plot
        for (int y = base; y < base + height; y++) {
            view.kinds.put(new BlockPos(x, y, z), Kind.WOOD);
            if (random.nextInt(4) == 0) {
                x += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
            }
        }
        int top = base + height;
        int radius = 1 + random.nextInt(4);
        for (int dy = -3; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = new BlockPos(x + dx, top + dy, z + dz);
                    if (!view.kinds.containsKey(pos) && random.nextInt(5) != 0) {
                        view.kinds.put(pos, Kind.CANOPY);
                    }
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            BlockPos stray = new BlockPos(random.nextInt(13) - 6, random.nextInt(top + 3), random.nextInt(13) - 6);
            view.kinds.putIfAbsent(stray, random.nextBoolean() ? Kind.NEVER_TOUCH : Kind.WOOD);
        }
        if (random.nextInt(8) == 0) {
            view.kinds.values().removeIf(kind -> kind == Kind.CANOPY); // a log build
        }
        return view;
    }

    private static TreeFeller.Limits limits(long seed) {
        TreeFeller.Limits limits = TreeFeller.Limits.within(Columns.square(ORIGIN, 9));
        return seed % 5 == 0 ? limits.withCap(60) : limits;
    }

    @Test
    @DisplayName("planning in small steps gives exactly the one-shot plan and the original algorithm's, reading no more")
    void resumedEqualsOneShot() {
        for (long seed = 0; seed < 300; seed++) {
            MapView oneShotView = randomTree(seed);
            TreeFeller.Limits limits = limits(seed);
            FellPlan oneShot = TreeFeller.plan(oneShotView, ORIGIN, limits);
            assertThat(oneShot).as("seed %d", seed).isEqualTo(ReferenceFeller.plan(randomTree(seed), ORIGIN, limits));

            for (int budget : new int[] {1, 3, 7, 64}) {
                MapView view = randomTree(seed);
                FellPlanner planner = new FellPlanner(view, ORIGIN, limits);
                FellPlan plan = null;
                int steps = 0;
                while (plan == null) {
                    int before = view.total;
                    plan = planner.step(budget);
                    assertThat(view.total - before).as("seed %d, budget %d", seed, budget).isLessThanOrEqualTo(budget);
                    steps++;
                    assertThat(steps).isLessThan(100_000);
                }
                assertThat(plan).as("seed %d, budget %d", seed, budget).isEqualTo(oneShot);
                assertThat(view.total).as("every position read once").isEqualTo(view.reads.size());
                assertThat(view.total).isEqualTo(oneShotView.total);
                assertThat(planner.reads()).isEqualTo(view.total);
                assertThat(planner.isDone()).isTrue();
                assertThat(planner.step(budget)).as("a done planner reads nothing more").isEqualTo(oneShot);
                assertThat(view.total).isEqualTo(oneShotView.total);
            }
        }
    }

    @Test
    @DisplayName("the seed probe reads at most five blocks: the plot and the four above it")
    void seedProbe() {
        MapView view = new MapView();
        FellPlanner planner = new FellPlanner(view, ORIGIN, TreeFeller.Limits.around(ORIGIN, 8));

        assertThat(planner.step(3)).isNull();
        assertThat(planner.hasSeed()).isFalse();
        assertThat(planner.step(100)).isEqualTo(FellPlan.skip(FellPlan.Reason.NO_TREE));
        assertThat(view.total).isEqualTo(TreeFeller.SEED_SEARCH_HEIGHT + 1);

        MapView tree = new MapView();
        tree.kinds.put(ORIGIN.above(2), Kind.WOOD);
        tree.kinds.put(ORIGIN.above(3), Kind.CANOPY);
        FellPlanner found = new FellPlanner(tree, ORIGIN, TreeFeller.Limits.around(ORIGIN, 8));
        assertThat(found.step(3)).isNull();
        assertThat(found.hasSeed()).as("the probe found the log on its third read").isTrue();
        assertThat(found.step(Integer.MAX_VALUE).blocks()).containsExactly(ORIGIN.above(3), ORIGIN.above(2));
    }

    @Test
    @DisplayName("a zero budget reads nothing and makes no progress")
    void zeroBudget() {
        MapView view = randomTree(7);
        FellPlanner planner = new FellPlanner(view, ORIGIN, limits(7));

        assertThat(planner.step(0)).isNull();
        assertThat(view.total).isZero();
        assertThat(planner.isDone()).isFalse();
    }

    /** {@link TreeFeller}'s algorithm as it was before planning became resumable. */
    private static final class ReferenceFeller {

        private static final int[][] FACE_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
        };

        static FellPlan plan(TreeView view, BlockPos plot, TreeFeller.Limits limits) {
            Map<BlockPos, Kind> kinds = new HashMap<>();
            TreeView cached = pos -> kinds.computeIfAbsent(pos, view::kind);
            BlockPos seed = findSeed(cached, plot.immutable());
            if (seed == null || !limits.wood().contains(seed)) {
                return FellPlan.skip(FellPlan.Reason.NO_TREE);
            }
            Set<BlockPos> wood = new HashSet<>();
            if (!fillWood(cached, seed, limits, wood)) {
                return FellPlan.skip(FellPlan.Reason.CAP_EXCEEDED);
            }
            Set<BlockPos> canopy = new HashSet<>();
            if (!fillCanopy(cached, seed, limits, wood, canopy)) {
                return FellPlan.skip(FellPlan.Reason.CAP_EXCEEDED);
            }
            if (canopy.isEmpty()) {
                return FellPlan.skip(FellPlan.Reason.NO_CANOPY);
            }
            List<BlockPos> ordered = new ArrayList<>(wood.size() + canopy.size());
            ordered.addAll(wood);
            ordered.addAll(canopy);
            ordered.sort(Comparator.comparingInt((BlockPos pos) -> -pos.getY())
                    .thenComparingInt(pos -> canopy.contains(pos) ? 0 : 1)
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));
            return FellPlan.found(ordered);
        }

        @Nullable
        private static BlockPos findSeed(TreeView view, BlockPos plot) {
            for (int dy = 0; dy <= TreeFeller.SEED_SEARCH_HEIGHT; dy++) {
                BlockPos pos = plot.above(dy);
                if (view.kind(pos) == Kind.WOOD) {
                    return pos;
                }
            }
            return null;
        }

        private static boolean fillWood(TreeView view, BlockPos seed, TreeFeller.Limits limits, Set<BlockPos> wood) {
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            wood.add(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            BlockPos next = pos.offset(dx, dy, dz);
                            if (wood.contains(next)
                                    || !inBounds(next, seed, limits, limits.wood())
                                    || view.kind(next) != Kind.WOOD) {
                                continue;
                            }
                            wood.add(next);
                            if (wood.size() > limits.cap()) {
                                return false;
                            }
                            queue.add(next);
                        }
                    }
                }
            }
            return true;
        }

        /** Explores the canopy, then keeps what is nearest this tree's wood, ties included. */
        private static boolean fillCanopy(
                TreeView view, BlockPos seed, TreeFeller.Limits limits, Set<BlockPos> wood, Set<BlockPos> canopy) {
            Map<BlockPos, Integer> explored = new HashMap<>();
            Set<BlockPos> other = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(wood);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                int step = explored.getOrDefault(pos, 0);
                if (step > FellPlanner.EXPLORE_REACH) {
                    continue;
                }
                for (int[] offset : FACE_OFFSETS) {
                    BlockPos next = pos.offset(offset[0], offset[1], offset[2]);
                    if (wood.contains(next) || explored.containsKey(next) || other.contains(next)
                            || !inCanopyBounds(next, seed, limits)) {
                        continue;
                    }
                    Kind kind = view.kind(next);
                    if (kind == Kind.WOOD) {
                        other.add(next);
                    } else if (kind == Kind.CANOPY) {
                        explored.put(next, step + 1);
                        if (explored.size() <= limits.cap()) {
                            queue.add(next);
                        }
                    }
                }
            }
            // Each explored block goes to its nearest wood; distances from each side compared.
            Map<BlockPos, Integer> fromOwn = distances(wood, explored.keySet());
            Map<BlockPos, Integer> fromOther = distances(other, explored.keySet());
            for (BlockPos pos : explored.keySet()) {
                int own = fromOwn.getOrDefault(pos, Integer.MAX_VALUE);
                if (own <= TreeFeller.CANOPY_REACH && own <= fromOther.getOrDefault(pos, Integer.MAX_VALUE)) {
                    canopy.add(pos);
                }
            }
            return wood.size() + canopy.size() <= limits.cap();
        }

        private static Map<BlockPos, Integer> distances(Set<BlockPos> sources, Set<BlockPos> through) {
            Map<BlockPos, Integer> distance = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(sources);
            sources.forEach(pos -> distance.put(pos, 0));
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                for (int[] offset : FACE_OFFSETS) {
                    BlockPos next = pos.offset(offset[0], offset[1], offset[2]);
                    if (through.contains(next) && !distance.containsKey(next)) {
                        distance.put(next, distance.get(pos) + 1);
                        queue.add(next);
                    }
                }
            }
            return distance;
        }

        private static boolean inCanopyBounds(BlockPos pos, BlockPos seed, TreeFeller.Limits limits) {
            return limits.canopy().contains(pos)
                    && pos.getY() >= seed.getY() - limits.below()
                    && pos.getY() <= seed.getY() + limits.above();
        }

        private static boolean inBounds(BlockPos pos, BlockPos seed, TreeFeller.Limits limits, Columns columns) {
            return columns.contains(pos)
                    && pos.getY() >= seed.getY() - limits.below()
                    && pos.getY() <= seed.getY() + limits.above();
        }
    }
}
