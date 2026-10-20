package com.logistics.forestry.logic;

import com.logistics.core.machine.area.Columns;
import net.minecraft.core.BlockPos;

/**
 * Plans which blocks make up the tree growing at a plot, without touching the world.
 *
 * <p>Wood is flood-filled from a seed log through all 26 neighbours, so diagonal trunks and
 * branches stay connected. Canopy is then filled from that wood through face neighbours only, up
 * to {@link #CANOPY_REACH} steps, and never steps from canopy into wood: touching leaves cannot
 * pull in a neighbouring trunk or a log building. Canopy is taken, and the fill goes on through it,
 * only when this tree's wood is (one of) its nearest, as vanilla judges a leaf's distance: in face
 * steps through canopy, to any wood. Canopy nearer other wood is left to it, so felling one of two
 * trees whose canopies touch takes only its own leaves; canopy as near both goes with the tree
 * felled first, and the other keeps the leaves beside its own wood. Distances are measured from the
 * wood found while planning, before any block is broken, never from the leaves' own recorded
 * {@code distance}, which updates only ticks after a nearby block changes. Wood with no canopy at
 * all is a player build and is skipped, as is anything larger than {@link Limits#cap()}.
 *
 * <p>The result is ordered top-down, so breaking it in order never leaves a block unsupported.
 */
public final class TreeFeller {

    /** How far above the plot to look for a trunk, e.g. a mangrove raised on its roots. */
    public static final int SEED_SEARCH_HEIGHT = 4;

    /** Maximum face-steps from the nearest wood block that canopy may be collected. */
    public static final int CANOPY_REACH = 6;

    /**
     * Search bounds and size cap. Wood must lie in the {@code wood} columns and canopy in the
     * {@code canopy} columns. Of the {@code keepOut} columns, only natural leaves that belong to the
     * tree ({@link TreeView#isLeaves}) are ever taken or walked through: no wood, no other canopy. Vertically, bounds are measured from
     * the seed log: {@code below} and {@code above}.
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

    /** Plans the tree at {@code plot} in one go; {@link FellPlanner} plans it in budgeted steps. */
    public static FellPlan plan(TreeView view, BlockPos plot, Limits limits) {
        return new FellPlanner(view, plot, limits).step(Integer.MAX_VALUE);
    }
}
