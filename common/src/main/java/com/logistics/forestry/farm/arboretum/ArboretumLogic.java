package com.logistics.forestry.farm.arboretum;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.area.PlotLayout;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.PlotAction;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The tree farm: plants saplings, lays soil, and fells grown trees whole.
 *
 * <p>Planting spots are single trunk cells of its {@link PlotLayout region}, any two at least
 * {@link #SPOT_SPACING} apart in Chebyshev distance, so neighbouring trunks never touch, not even
 * diagonally. Spots are chosen greedily from lane {@link #FIRST_PLANTED_LANE} outward, each lane
 * walked in step order: a plot becomes a spot iff it keeps that spacing from every spot already
 * chosen, measured on the plots' real positions. Lane 0, beside the courtyard the ring surrounds,
 * is never planted. A lane's spots depend only on the lanes inside it, so widening the ring only
 * adds spots in the new lanes: every existing spot stays.
 *
 * <p>Saplings that only grow as a 2×2 group ({@code logistics:forestry/requires_2x2}, e.g. dark
 * oak) are not germlings here; a 2×2 tree that grows in the ring anyway is felled like any other.
 *
 * <p>Each plot gets, in order: its tree felled, off the spots only one standing on the plot itself; its soil replaced with Humus unless it is Humus
 * already; on a spot, a sapling planted. Humus is the only soil the Arboretum lays, so without it
 * unprepared plots report no soil and stay unplanted. A felled tree leaves sand where its Humus
 * was, which the next pass digs out and relays.
 */
public final class ArboretumLogic implements FarmLogic {

    /** Least Chebyshev distance between two planting spots. */
    static final int SPOT_SPACING = 2;

    /** Innermost lane that holds spots; the lanes inside it stay bare soil. */
    static final int FIRST_PLANTED_LANE = 1;

    /** Regions whose spots are remembered; beyond this the least recently used are recomputed. */
    private static final int SPOT_CACHE_SIZE = 64;

    /** How far from a plot outside any region a tree may reach. */
    static final int FREE_STANDING_RADIUS = 8;

    private final Predicate<ItemStack> germlings;
    private final Predicate<ItemStack> soils;
    private final Predicate<ItemStack> requires2x2;
    private final Predicate<ItemStack> humus;
    private final Predicate<BlockState> ownGrowth;
    private final Map<PlotLayout, BitSet> spotCache =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<PlotLayout, BitSet> eldest) {
                    return size() > SPOT_CACHE_SIZE;
                }
            });

    public ArboretumLogic() {
        this(
                stack -> stack.is(LogisticsForestry.TAG.ARBORETUM_GERMLINGS),
                stack -> stack.is(LogisticsForestry.TAG.ARBORETUM_SOILS),
                stack -> stack.is(LogisticsForestry.TAG.REQUIRES_2X2),
                stack -> stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()),
                state -> state.is(BlockTags.SAPLINGS)
                        || state.is(LevelTreeView.FELLABLE_WOOD)
                        || state.is(LevelTreeView.FELLABLE_CANOPY));
    }

    /** Tags are not bound in unit tests, so the item and block sets are injectable. */
    ArboretumLogic(
            Predicate<ItemStack> germlings,
            Predicate<ItemStack> soils,
            Predicate<ItemStack> requires2x2,
            Predicate<ItemStack> humus,
            Predicate<BlockState> ownGrowth) {
        this.germlings = germlings;
        this.soils = soils;
        this.requires2x2 = requires2x2;
        this.humus = humus;
        this.ownGrowth = ownGrowth;
    }

    @Override
    public int clearanceHeight() {
        return 10;
    }

    /** Saplings, and the wood and canopy of trees, propagules grounded or hanging included. */
    @Override
    public boolean isOwnGrowth(BlockState state) {
        return ownGrowth.test(state);
    }

    @Override
    public boolean laysSoil() {
        return true;
    }

    /** Humus under a felled tree is spent and turns to sand. */
    @Override
    @Nullable
    public BlockState soilAfterHarvest(BlockState soil) {
        return isPrepared(soil) ? Blocks.SAND.defaultBlockState() : null;
    }

    @Override
    public boolean replantsFromOutput() {
        return true;
    }

    /** Saplings needing a 2×2 group are refused. */
    @Override
    public boolean isGermling(ItemStack stack) {
        return !stack.isEmpty() && germlings.test(stack) && !requires2x2.test(stack);
    }

    @Override
    public boolean isSoil(ItemStack stack) {
        return !stack.isEmpty() && soils.test(stack);
    }

    /**
     * Without a region the plot stands alone: a planting spot, felling a tree within
     * {@link #FREE_STANDING_RADIUS} of it.
     */
    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        return decide(world, plot, true, TreeFeller.Limits.around(plot, FREE_STANDING_RADIUS), inputs);
    }

    @Override
    public PlotAction decide(FarmWorld world, PlotLayout region, BlockPos plot, FarmInputs inputs) {
        TreeFeller.Limits limits = TreeFeller.Limits.within(region.bounds(), region.keepOut());
        int index = region.indexOf(plot);
        if (index < 0) {
            FellPlan tree = world.fellPlan(plot, limits);
            return tree.isFound() ? new PlotAction.Harvest(tree.blocks()) : PlotAction.NONE;
        }
        return decide(world, plot, spots(region).get(index), limits, inputs);
    }

    private PlotAction decide(
            FarmWorld world, BlockPos plot, boolean spot, TreeFeller.Limits limits, FarmInputs inputs) {
        FellPlan tree = world.fellPlan(plot, limits);
        // Off the spots only a tree standing on the plot is felled: a plan seeded at a branch hanging
        // over it would miss the trunk below and strand it without a canopy.
        if (tree.isFound() && (spot || world.treeView().kind(plot) == TreeView.Kind.WOOD)) {
            return new PlotAction.Harvest(tree.blocks());
        }
        if (!isOpen(world.state(plot))) {
            return PlotAction.NONE;
        }
        BlockPos ground = plot.below();
        if (world.isLoaded(ground) && !isPrepared(world.state(ground))) {
            return new PlotAction.PlaceSoil(ground);
        }
        if (!spot) {
            return PlotAction.NONE;
        }
        return new PlotAction.Plant(List.of(plot), germling(inputs));
    }

    // ==================== Pattern ====================

    /** The region indices of {@code region}'s planting spots. */
    BitSet spots(PlotLayout region) {
        return spotCache.computeIfAbsent(region, ArboretumLogic::chooseSpots);
    }

    /**
     * Walks the lanes from {@link #FIRST_PLANTED_LANE} outward, each in step order, and takes every
     * plot that keeps {@link #SPOT_SPACING} from the spots taken before it.
     */
    static BitSet chooseSpots(PlotLayout region) {
        BitSet spots = new BitSet();
        Set<Long> taken = new HashSet<>();
        for (int lane = FIRST_PLANTED_LANE; lane < region.laneCount(); lane++) {
            for (int step = 0; step < region.laneLength(lane); step++) {
                BlockPos cell = region.plot(lane, step);
                if (keepsSpacing(taken, cell)) {
                    taken.add(column(cell.getX(), cell.getZ()));
                    spots.set(region.indexOf(cell));
                }
            }
        }
        return spots;
    }

    private static boolean keepsSpacing(Set<Long> taken, BlockPos cell) {
        int reach = SPOT_SPACING - 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (taken.contains(column(cell.getX() + dx, cell.getZ() + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // ==================== Plant and soil ====================

    /** The first germling on hand, or empty when there is none: the farm reports it is out of germlings. */
    private ItemStack germling(FarmInputs inputs) {
        for (ItemStack kind : inputs.germlings()) {
            if (!kind.isEmpty() && !requires2x2.test(kind)) {
                return kind;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Only Humus is prepared soil. */
    private boolean isPrepared(BlockState ground) {
        return humus.test(new ItemStack(ground.getBlock()));
    }

    // ==================== Helpers ====================

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }
}
