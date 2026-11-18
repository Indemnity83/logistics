package com.logistics.forestry.farm.orchard;

import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmUpkeep;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Orchard: picks fruit off living trees, never breaking, planting or repairing a block. Each tree
 * whose trunk base, the lowest log of its trunk, stands in the Reach's lowest levels is a Picking
 * Plot, while at least one of its leaves bears a fruit ({@link OrchardFruits}): natural, still
 * attached to the tree ({@code distance} under {@link LeavesBlock#DECAY_DISTANCE}), and matching the
 * fruit's {@code leaves}. A tree on any ground counts.
 *
 * <p>A tree is read the way the Arboretum fells one ({@link TreeFeller}): its wood within two columns
 * of the trunk base, and its canopy, however far above or beyond the Reach, by vanilla's nearest-log
 * rule, so leaves nearer another tree's wood are that tree's. A trunk of several logs side by side
 * (dark oak) is one tree, kept at the base lowest, then furthest west, then north.
 *
 * <p>When the leaves of one tree bear several fruits, the first definition in load order wins.
 */
public final class OrchardTendLogic implements TendLogic {

    /** How far the Orchard's Reach extends above it, to see the trees it picks from. */
    public static final int REACH_ABOVE = 7;

    /** How far from the trunk base a tree's wood is read, for side-by-side trunks and branches. */
    static final int WOOD_RADIUS = 2;

    /** How far down a log is followed to the base of its trunk. */
    private static final int TRUNK_DEPTH = REACH_ABOVE + 2;

    private static final Comparator<BlockPos> BASE_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ);

    private static final Set<FarmCost> ACTIONS = EnumSet.of(FarmCost.HARVEST_CROP);

    private final Supplier<List<OrchardFruit>> fruits;

    public OrchardTendLogic() {
        this(OrchardFruits::all);
    }

    /** Test seam: {@code fruits} stands in for the loaded fruit definitions. */
    public OrchardTendLogic(Supplier<List<OrchardFruit>> fruits) {
        this.fruits = fruits;
    }

    /** Never: the Orchard breaks nothing. */
    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        return null;
    }

    @Override
    public int reachAbove() {
        return REACH_ABOVE;
    }

    @Override
    public boolean picks() {
        return true;
    }

    /** The base of the trunk a log at {@code pos} is part of, while its tree bears fruit. */
    @Override
    @Nullable
    public BlockPos pickPlot(FarmWorld world, BlockPos pos) {
        if (!isWood(world, pos)) {
            return null;
        }
        BlockPos base = pos;
        for (int i = 0; i < TRUNK_DEPTH && isWood(world, base.below()); i++) {
            base = base.below();
        }
        Tree tree = tree(world, base);
        if (tree == null || bearing(world, tree) == null) {
            return null;
        }
        return tree.base();
    }

    /** The fruit the tree on {@code plot} bears; {@code null} once it is gone or bears none. */
    @Override
    @Nullable
    public Bearing bearing(FarmWorld world, BlockPos plot) {
        if (!isWood(world, plot) || isWood(world, plot.below())) {
            return null;
        }
        Tree tree = tree(world, plot);
        return tree != null && tree.base().equals(plot) ? bearing(world, tree) : null;
    }

    @Override
    public FarmUpkeep upkeep() {
        return FarmUpkeep.ORCHARD;
    }

    @Override
    public Set<FarmCost> actions() {
        return ACTIONS;
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** Never: the Orchard keeps no Plot that is planted. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState ground) {
        return null;
    }

    /** A tree read from a log of its trunk base: its lowest, westmost, northmost base, and its canopy. */
    private record Tree(BlockPos base, List<BlockPos> canopy) {}

    /** The tree whose trunk has a base at {@code base}; {@code null} when it is no tree. */
    @Nullable
    private static Tree tree(FarmWorld world, BlockPos base) {
        FellPlan plan = world.fellPlan(base, limits(base));
        if (!plan.isFound()) {
            return null;
        }
        Set<BlockPos> wood = new HashSet<>();
        List<BlockPos> canopy = new ArrayList<>();
        for (BlockPos pos : plan.blocks()) {
            if (isWood(world, pos)) {
                wood.add(pos);
            } else {
                canopy.add(pos);
            }
        }
        BlockPos lowest = base;
        for (BlockPos log : wood) {
            if (!wood.contains(log.below()) && BASE_ORDER.compare(log, lowest) < 0) {
                lowest = log;
            }
        }
        return new Tree(lowest, canopy);
    }

    /** The first fruit any of {@code tree}'s leaves bear, picked from those leaves. */
    @Nullable
    private Bearing bearing(FarmWorld world, Tree tree) {
        List<BlockState> states = new ArrayList<>(tree.canopy().size());
        for (BlockPos pos : tree.canopy()) {
            states.add(world.state(pos));
        }
        for (OrchardFruit fruit : fruits.get()) {
            List<BlockPos> bearing = new ArrayList<>();
            for (int i = 0; i < states.size(); i++) {
                BlockState state = states.get(i);
                if (isAttachedLeaves(state) && fruit.bears(state)) {
                    bearing.add(tree.canopy().get(i));
                }
            }
            if (!bearing.isEmpty()) {
                return new Bearing(fruit.stack(), fruit.chance(), FarmCost.HARVEST_CROP, bearing);
            }
        }
        return null;
    }

    /** Natural leaves still attached to a tree: they would not decay. */
    static boolean isAttachedLeaves(BlockState state) {
        return LevelTreeView.isNaturalLeaves(state)
                && state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE;
    }

    private static boolean isWood(FarmWorld world, BlockPos pos) {
        return world.isLoaded(pos) && world.treeKind(world.state(pos)) == TreeView.Kind.WOOD;
    }

    /** Wood near the trunk base and no lower; canopy around it, however high. */
    private static TreeFeller.Limits limits(BlockPos base) {
        Columns wood = Columns.square(base, 2 * WOOD_RADIUS + 1);
        return new TreeFeller.Limits(
                wood, wood.grow(TreeFeller.Limits.CANOPY_MARGIN), 0, TreeFeller.Limits.ABOVE, TreeFeller.Limits.CAP);
    }
}
