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
import java.util.List;
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
 * <p>Planting pattern, by a plot's lane and step in its {@link PlotLayout region} (lane 0 innermost,
 * steps clockwise from the north-west corner):
 *
 * <ul>
 *   <li><b>Single saplings</b> go on every even step of every lane. Neighbouring steps of a lane,
 *       and the cells of neighbouring lanes beside them, always differ in parity, so no two
 *       single spots are orthogonally adjacent and half the ring is single spots.
 *   <li><b>2×2 saplings</b> ({@code logistics:forestry/requires_2x2}, e.g. dark oak) go in aligned
 *       groups spanning lanes 0 and 1, anchored on lane 0. Every corner holds one: lane 0's corner
 *       with lane 1's corner and the two cells beside it. Along each edge, groups two cells long
 *       follow, one cell apart from each other and from the corner groups. A group is planted all
 *       or none, and only once four of the same sapling are on hand. A 2×2 sapling is never planted
 *       on its own, and a ring one lane wide has no groups.
 * </ul>
 *
 * At an anchor a 2×2 sapling is preferred over a single one. Each plot gets, in order: its tree
 * felled; its soil replaced with Humus unless it is Humus already; a sapling planted. Humus is the
 * only soil the Arboretum lays, so without it unprepared plots report no soil and stay unplanted.
 * A felled tree leaves sand where its Humus was, which the next pass digs out and relays.
 */
public final class ArboretumLogic implements FarmLogic {

    /** Along an edge, group anchors fall every this many steps of lane 0. */
    static final int GROUP_SPACING = 3;

    /** How far from a plot outside any region a tree may reach. */
    static final int FREE_STANDING_RADIUS = 8;

    private final Predicate<ItemStack> germlings;
    private final Predicate<ItemStack> soils;
    private final Predicate<ItemStack> requires2x2;
    private final Predicate<ItemStack> humus;
    private final Predicate<BlockState> ownGrowth;

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

    @Override
    public boolean isGermling(ItemStack stack) {
        return !stack.isEmpty() && germlings.test(stack);
    }

    @Override
    public boolean isSoil(ItemStack stack) {
        return !stack.isEmpty() && soils.test(stack);
    }

    /**
     * Without a region the plot stands alone: a single-sapling spot with no group, felling a tree
     * within {@link #FREE_STANDING_RADIUS} of it.
     */
    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        return decide(world, plot, true, null, TreeFeller.Limits.around(plot, FREE_STANDING_RADIUS), inputs);
    }

    @Override
    public PlotAction decide(FarmWorld world, PlotLayout region, BlockPos plot, FarmInputs inputs) {
        TreeFeller.Limits limits = TreeFeller.Limits.within(region.bounds(), region.keepOut());
        int index = region.indexOf(plot);
        if (index < 0) {
            FellPlan tree = TreeFeller.plan(world.treeView(), plot, limits);
            return tree.isFound() ? new PlotAction.Harvest(tree.blocks()) : PlotAction.NONE;
        }
        int lane = region.lane(index);
        int step = region.step(index);
        return decide(world, plot, isSingleSpot(lane, step), group(region, lane, step), limits, inputs);
    }

    private PlotAction decide(
            FarmWorld world,
            BlockPos plot,
            boolean single,
            @Nullable List<BlockPos> group,
            TreeFeller.Limits limits,
            FarmInputs inputs) {
        FellPlan tree = TreeFeller.plan(world.treeView(), plot, limits);
        if (tree.isFound()) {
            return new PlotAction.Harvest(tree.blocks());
        }
        if (!isOpen(world.state(plot))) {
            return PlotAction.NONE;
        }
        BlockPos ground = plot.below();
        if (world.isLoaded(ground) && !isPrepared(world.state(ground))) {
            return new PlotAction.PlaceSoil(ground);
        }

        Planting planting = choosePlanting(world, plot, single, group, inputs);
        if (planting == null) {
            return PlotAction.NONE;
        }
        if (!planting.germling().isEmpty()) {
            for (BlockPos cell : planting.cells()) {
                if (!isPrepared(world.state(cell.below()))) {
                    return new PlotAction.PlaceSoil(cell.below());
                }
            }
        }
        return new PlotAction.Plant(planting.cells(), planting.germling());
    }

    // ==================== Pattern ====================

    /** Every even step of every lane. */
    static boolean isSingleSpot(int lane, int step) {
        return step % 2 == 0;
    }

    /**
     * The 2×2 group anchored at ({@code lane}, {@code step}), anchor first, or {@code null} when no
     * group is anchored there or any of its cells lies outside {@code region}.
     */
    @Nullable
    static List<BlockPos> group(PlotLayout region, int lane, int step) {
        List<BlockPos> group = groupCells(region, lane, step);
        if (group == null) {
            return null;
        }
        for (BlockPos cell : group) {
            if (!region.contains(cell)) {
                return null;
            }
        }
        return group;
    }

    @Nullable
    private static List<BlockPos> groupCells(PlotLayout region, int lane, int step) {
        if (lane != 0 || region.laneCount() < 2) {
            return null;
        }
        int side = region.laneLength(0) / 4;
        int outerSide = region.laneLength(1) / 4;
        int edge = step / side;
        int along = step % side;
        // Lane 1's corner at the start of this edge; lane 1 runs one cell further out on each side.
        int outerCorner = edge * outerSide;
        if (along == 0) {
            int before = Math.floorMod(outerCorner - 1, region.laneLength(1));
            return List.of(
                    region.plot(0, step),
                    region.plot(1, outerCorner),
                    region.plot(1, outerCorner + 1),
                    region.plot(1, before));
        }
        if (along >= 2 && (along - 2) % GROUP_SPACING == 0 && along + 1 <= side - 2) {
            return List.of(
                    region.plot(0, step),
                    region.plot(0, step + 1),
                    region.plot(1, outerCorner + along + 1),
                    region.plot(1, outerCorner + along + 2));
        }
        return null;
    }

    // ==================== Plant and soil ====================

    private record Planting(List<BlockPos> cells, ItemStack germling) {}

    /**
     * What to plant at this plot, or {@code null} when nothing should go here. An empty germling
     * means the plot wants planting but nothing suitable is on hand.
     */
    @Nullable
    private Planting choosePlanting(
            FarmWorld world, BlockPos plot, boolean single, @Nullable List<BlockPos> group, FarmInputs inputs) {
        List<ItemStack> available = inputs.germlings();

        ItemStack shortGroup = ItemStack.EMPTY;
        if (group != null && allOpen(world, group)) {
            for (ItemStack kind : available) {
                if (!requires2x2.test(kind)) {
                    continue;
                }
                if (kind.getCount() >= group.size()) {
                    return new Planting(group, kind);
                }
                if (shortGroup.isEmpty()) {
                    shortGroup = kind;
                }
            }
        }
        if (single) {
            for (ItemStack kind : available) {
                if (!requires2x2.test(kind)) {
                    return new Planting(List.of(plot), kind);
                }
            }
        }
        if (!shortGroup.isEmpty()) {
            // A group short of saplings: the farm reports it is out of germlings.
            return new Planting(group, shortGroup);
        }
        if (single && available.isEmpty()) {
            return new Planting(List.of(plot), ItemStack.EMPTY);
        }
        return null;
    }

    /** Only Humus is prepared soil. */
    private boolean isPrepared(BlockState ground) {
        return humus.test(new ItemStack(ground.getBlock()));
    }

    // ==================== Helpers ====================

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean allOpen(FarmWorld world, List<BlockPos> cells) {
        for (BlockPos cell : cells) {
            if (!world.isLoaded(cell) || !isOpen(world.state(cell))) {
                return false;
            }
        }
        return true;
    }
}
