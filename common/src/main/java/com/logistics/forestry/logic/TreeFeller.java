package com.logistics.forestry.logic;

import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.logic.TreeView.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Plans which blocks make up the tree growing at a plot, without touching the world.
 *
 * <p>Wood is flood-filled from a seed log through all 26 neighbours, so diagonal trunks and
 * branches stay connected. Canopy is then filled from that wood through face neighbours only, up
 * to {@link #CANOPY_REACH} steps, and never steps from canopy into wood: touching leaves cannot
 * pull in a neighbouring trunk or a log building. Wood with no canopy at all is a player build
 * and is skipped, as is anything larger than {@link Limits#cap()}.
 *
 * <p>The result is ordered top-down, so breaking it in order never leaves a block unsupported.
 */
public final class TreeFeller {

    /** How far above the plot to look for a trunk, e.g. a mangrove raised on its roots. */
    public static final int SEED_SEARCH_HEIGHT = 4;

    /** Maximum face-steps from the nearest wood block that canopy may be collected. */
    public static final int CANOPY_REACH = 6;

    private static final int[][] FACE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] ALL_OFFSETS = allOffsets();

    /**
     * Search bounds and size cap. Wood must lie in the {@code wood} columns and canopy in the
     * {@code canopy} columns, and nothing is ever taken from or walked through the {@code keepOut}
     * columns. Vertically, bounds are measured from the seed log: {@code below} and {@code above}.
     */
    public record Limits(Columns wood, Columns canopy, Columns keepOut, int below, int above, int cap) {
        public static final int BELOW = 3;
        public static final int ABOVE = 32;
        public static final int CAP = 1024;

        /** How far canopy may reach beyond the wood bounds, e.g. leaves hanging past a farm's edge. */
        public static final int CANOPY_MARGIN = 3;

        public Limits {
            if (wood == null || canopy == null || keepOut == null || below < 0 || above < 0 || cap < 1) {
                throw new IllegalArgumentException("invalid tree felling limits: " + this);
            }
        }

        /**
         * A tree worked from a region: wood inside {@code bounds}, canopy inside them widened by
         * {@link #CANOPY_MARGIN}, never anything in {@code keepOut}.
         */
        public static Limits within(Columns bounds, Columns keepOut) {
            return new Limits(bounds, bounds.grow(CANOPY_MARGIN), keepOut, BELOW, ABOVE, CAP);
        }

        /** A free-standing tree: wood and canopy within {@code radius} columns of {@code plot}. */
        public static Limits around(BlockPos plot, int radius) {
            if (radius < 0) {
                throw new IllegalArgumentException("radius must be >= 0: " + radius);
            }
            Columns square = Columns.square(plot, 2 * radius + 1);
            return new Limits(square, square, Columns.EMPTY, BELOW, ABOVE, CAP);
        }

        public Limits withCap(int cap) {
            return new Limits(wood, canopy, keepOut, below, above, cap);
        }

        public Limits withHeight(int below, int above) {
            return new Limits(wood, canopy, keepOut, below, above, cap);
        }
    }

    private TreeFeller() {}

    public static FellPlan plan(TreeView view, BlockPos plot, Limits limits) {
        Map<BlockPos, Kind> kinds = new HashMap<>();
        TreeView cached = pos -> kinds.computeIfAbsent(pos, view::kind);

        BlockPos seed = findSeed(cached, plot.immutable());
        if (seed == null || !limits.wood().contains(seed) || limits.keepOut().contains(seed)) {
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

    private static @Nullable BlockPos findSeed(TreeView view, BlockPos plot) {
        for (int dy = 0; dy <= SEED_SEARCH_HEIGHT; dy++) {
            BlockPos pos = plot.above(dy);
            if (view.kind(pos) == Kind.WOOD) {
                return pos;
            }
        }
        return null;
    }

    /** Fills {@code wood}; returns false once the cap is exceeded. */
    private static boolean fillWood(TreeView view, BlockPos seed, Limits limits, Set<BlockPos> wood) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        wood.add(seed);
        queue.add(seed);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (int[] offset : ALL_OFFSETS) {
                BlockPos next = pos.offset(offset[0], offset[1], offset[2]);
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
        return true;
    }

    /** Fills {@code canopy} breadth-first from all wood at once; returns false once the cap is exceeded. */
    private static boolean fillCanopy(
            TreeView view, BlockPos seed, Limits limits, Set<BlockPos> wood, Set<BlockPos> canopy) {
        Map<BlockPos, Integer> steps = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(wood);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int step = steps.getOrDefault(pos, 0);
            if (step >= CANOPY_REACH) {
                continue;
            }
            for (int[] offset : FACE_OFFSETS) {
                BlockPos next = pos.offset(offset[0], offset[1], offset[2]);
                if (wood.contains(next)
                        || canopy.contains(next)
                        || !inBounds(next, seed, limits, limits.canopy())
                        || view.kind(next) != Kind.CANOPY) {
                    continue;
                }
                canopy.add(next);
                if (wood.size() + canopy.size() > limits.cap()) {
                    return false;
                }
                steps.put(next, step + 1);
                queue.add(next);
            }
        }
        return true;
    }

    private static boolean inBounds(BlockPos pos, BlockPos seed, Limits limits, Columns columns) {
        return columns.contains(pos)
                && !limits.keepOut().contains(pos)
                && pos.getY() >= seed.getY() - limits.below()
                && pos.getY() <= seed.getY() + limits.above();
    }

    private static int[][] allOffsets() {
        List<int[]> offsets = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        offsets.add(new int[] {dx, dy, dz});
                    }
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }
}
