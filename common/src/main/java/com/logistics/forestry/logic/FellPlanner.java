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
 * {@link TreeFeller}'s planning, resumable: each {@link #step} reads at most its budget of new
 * positions and picks up where the last one stopped, so a large tree can be planned over several
 * ticks. The frontier, the visited sets and every position read so far stay in memory; nothing is
 * read twice. Driven to completion it gives exactly {@link TreeFeller#plan}'s answer, which runs
 * through it.
 *
 * <p>Phases: a seed probe (at most {@link TreeFeller#SEED_SEARCH_HEIGHT} + 1 reads up from the
 * plot), the wood fill through all 26 neighbours, then the canopy fill through face neighbours.
 */
public final class FellPlanner {

    private enum Phase {
        SEED,
        WOOD,
        CANOPY,
        DONE
    }

    private static final int[][] FACE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] ALL_OFFSETS = allOffsets();

    /**
     * How far from this tree's wood the canopy is explored: far enough to meet any other wood nearer
     * to a leaf within {@link TreeFeller#CANOPY_REACH}.
     */
    static final int EXPLORE_REACH = 2 * TreeFeller.CANOPY_REACH - 2;

    private final TreeView view;
    private final BlockPos plot;
    private final TreeFeller.Limits limits;
    private final boolean requireCanopy;
    private final Map<BlockPos, Kind> kinds = new HashMap<>();
    private final Set<BlockPos> wood = new HashSet<>();
    private final Set<BlockPos> canopy = new HashSet<>();
    private final Map<BlockPos, Integer> steps = new HashMap<>();
    // Canopy explored with its steps from this tree's wood, and other wood beside it.
    private final Map<BlockPos, Integer> explored = new HashMap<>();
    private final Set<BlockPos> otherWood = new HashSet<>();
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

    private Phase phase = Phase.SEED;
    private int seedDy;
    @Nullable private BlockPos seed;
    // The node being expanded and the index of its next neighbour.
    @Nullable private BlockPos current;
    private int offset;
    @Nullable private FellPlan result;
    private int budget;
    private int reads;

    public FellPlanner(TreeView view, BlockPos plot, TreeFeller.Limits limits) {
        this(view, plot, limits, true);
    }

    /**
     * A planner that, without {@code requireCanopy}, also takes wood standing bare: the rest of a
     * tree whose canopy has already been felled.
     */
    public FellPlanner(TreeView view, BlockPos plot, TreeFeller.Limits limits, boolean requireCanopy) {
        this.view = view;
        this.plot = plot.immutable();
        this.limits = limits;
        this.requireCanopy = requireCanopy;
    }

    public BlockPos plot() {
        return plot;
    }

    public TreeFeller.Limits limits() {
        return limits;
    }

    /** Positions read so far, over every step. */
    public int reads() {
        return reads;
    }

    /** Whether the seed probe is over and found a log the tree may grow from. */
    public boolean hasSeed() {
        return phase != Phase.SEED && seed != null;
    }

    public boolean isDone() {
        return phase == Phase.DONE;
    }

    /** The finished plan, or {@code null} while planning is still in progress. */
    @Nullable
    public FellPlan result() {
        return result;
    }

    /**
     * Plans on, reading at most {@code readBudget} positions not read before.
     *
     * @return the finished plan, or {@code null} if the budget ran out first
     */
    @Nullable
    public FellPlan step(int readBudget) {
        if (phase == Phase.DONE) {
            return result;
        }
        budget = Math.max(0, readBudget);
        if (phase == Phase.SEED && !probeSeed()) {
            return null;
        }
        if (phase == Phase.WOOD && !fillWood()) {
            return null;
        }
        if (phase == Phase.CANOPY && !fillCanopy()) {
            return null;
        }
        return result;
    }

    /** What {@code pos} is, or {@code null} when it has not been read and the budget is spent. */
    @Nullable
    private Kind look(BlockPos pos) {
        Kind known = kinds.get(pos);
        if (known != null) {
            return known;
        }
        if (budget <= 0) {
            return null;
        }
        budget--;
        reads++;
        Kind kind = view.kind(pos);
        kinds.put(pos.immutable(), kind);
        return kind;
    }

    private boolean probeSeed() {
        while (seedDy <= TreeFeller.SEED_SEARCH_HEIGHT) {
            BlockPos pos = plot.above(seedDy);
            Kind kind = look(pos);
            if (kind == null) {
                return false;
            }
            if (kind == Kind.WOOD) {
                seed = pos;
                break;
            }
            seedDy++;
        }
        if (seed == null || !limits.wood().contains(seed)) {
            seed = null;
            finish(FellPlan.skip(FellPlan.Reason.NO_TREE));
            return true;
        }
        wood.add(seed);
        queue.add(seed);
        phase = Phase.WOOD;
        return true;
    }

    /** Fills the wood; false when the budget ran out first. */
    private boolean fillWood() {
        while (true) {
            if (current == null) {
                current = queue.poll();
                offset = 0;
                if (current == null) {
                    queue.addAll(wood);
                    phase = Phase.CANOPY;
                    return true;
                }
            }
            while (offset < ALL_OFFSETS.length) {
                int[] o = ALL_OFFSETS[offset];
                BlockPos next = current.offset(o[0], o[1], o[2]);
                if (wood.contains(next) || !inCanopyBounds(next, limits.wood())) {
                    offset++;
                    continue;
                }
                Kind kind = look(next);
                if (kind == null) {
                    return false;
                }
                offset++;
                if (kind != Kind.WOOD) {
                    continue;
                }
                wood.add(next);
                if (wood.size() > limits.cap()) {
                    finish(FellPlan.skip(FellPlan.Reason.CAP_EXCEEDED));
                    return true;
                }
                queue.add(next);
            }
            current = null;
        }
    }

    /**
     * Explores the canopy breadth-first from all of this tree's wood, through face neighbours, far
     * enough to find any other wood nearer to a leaf within reach; false when the budget ran out
     * first. Then keeps the canopy whose nearest wood is this tree's.
     */
    private boolean fillCanopy() {
        while (true) {
            if (current == null) {
                current = queue.poll();
                offset = 0;
                if (current == null) {
                    claimCanopy();
                    return true;
                }
                if (steps.getOrDefault(current, 0) > EXPLORE_REACH) {
                    current = null;
                    continue;
                }
            }
            int step = steps.getOrDefault(current, 0);
            while (offset < FACE_OFFSETS.length) {
                int[] o = FACE_OFFSETS[offset];
                BlockPos next = current.offset(o[0], o[1], o[2]);
                if (wood.contains(next) || explored.containsKey(next) || otherWood.contains(next)
                        || !inCanopyBounds(next)) {
                    offset++;
                    continue;
                }
                Kind kind = look(next);
                if (kind == null) {
                    return false;
                }
                offset++;
                if (kind == Kind.WOOD) {
                    otherWood.add(next.immutable());
                    continue;
                }
                if (kind != Kind.CANOPY) {
                    continue;
                }
                explored.put(next.immutable(), step + 1);
                if (explored.size() <= limits.cap()) {
                    steps.put(next, step + 1);
                    queue.add(next);
                }
            }
            current = null;
        }
    }

    /**
     * Gives each explored canopy block to its nearest wood, in face steps through canopy, this
     * tree's winning ties, and takes this tree's within {@link TreeFeller#CANOPY_REACH}. Reads nothing.
     */
    private void claimCanopy() {
        Map<BlockPos, Integer> distance = new HashMap<>();
        Set<BlockPos> own = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        for (BlockPos log : wood) {
            distance.put(log, 0);
            own.add(log);
            frontier.add(log);
        }
        for (BlockPos log : otherWood) {
            distance.put(log, 0);
            frontier.add(log);
        }
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            int next = distance.get(pos) + 1;
            boolean ours = own.contains(pos);
            for (int[] o : FACE_OFFSETS) {
                BlockPos beside = pos.offset(o[0], o[1], o[2]);
                if (!explored.containsKey(beside)) {
                    continue;
                }
                Integer known = distance.get(beside);
                if (known == null) {
                    distance.put(beside, next);
                    if (ours) {
                        own.add(beside);
                    }
                    frontier.add(beside);
                } else if (ours && known == next) {
                    own.add(beside);
                }
            }
        }
        for (BlockPos pos : explored.keySet()) {
            if (own.contains(pos) && distance.get(pos) <= TreeFeller.CANOPY_REACH) {
                canopy.add(pos);
            }
        }
        if (wood.size() + canopy.size() > limits.cap()) {
            finish(FellPlan.skip(FellPlan.Reason.CAP_EXCEEDED));
        } else if (canopy.isEmpty() && requireCanopy) {
            finish(FellPlan.skip(FellPlan.Reason.NO_CANOPY));
        } else {
            finish(FellPlan.found(ordered()));
        }
    }

    private List<BlockPos> ordered() {
        List<BlockPos> ordered = new ArrayList<>(wood.size() + canopy.size());
        ordered.addAll(wood);
        ordered.addAll(canopy);
        ordered.sort(Comparator.comparingInt((BlockPos pos) -> -pos.getY())
                .thenComparingInt(pos -> canopy.contains(pos) ? 0 : 1)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        return ordered;
    }

    private void finish(FellPlan plan) {
        result = plan;
        phase = Phase.DONE;
        current = null;
        queue.clear();
        steps.clear();
        explored.clear();
        otherWood.clear();
        kinds.clear();
        wood.clear();
        canopy.clear();
    }

    private boolean inCanopyBounds(BlockPos pos) {
        return inCanopyBounds(pos, limits.canopy());
    }

    private boolean inCanopyBounds(BlockPos pos, Columns columns) {
        return columns.contains(pos)
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
