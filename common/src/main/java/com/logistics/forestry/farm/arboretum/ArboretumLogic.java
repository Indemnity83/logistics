package com.logistics.forestry.farm.arboretum;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.area.WorkArea;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.PlotAction;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.TreeFeller;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The tree farm: plants saplings, lays soil, and fells grown trees whole.
 *
 * <p>Planting pattern, by a plot's row and column in the {@link WorkArea} (0-based, rows from the
 * near edge outward, columns left to right looking along the farm's facing):
 *
 * <ul>
 *   <li><b>Single saplings</b> go on a spacing-2 lattice: row and column both even.
 *   <li><b>2×2 saplings</b> ({@code logistics:forestry/requires_2x2}, e.g. dark oak) go in aligned
 *       groups anchored where row and column are both multiples of 3 and the group's other three
 *       cells (row+1, column+1) are inside the area. A group is planted all or none, and only once
 *       four of the same sapling are on hand. A 2×2 sapling is never planted on its own.
 * </ul>
 *
 * At an anchor a 2×2 sapling is preferred over a single one. Each plot gets, in order: its tree
 * felled; soil laid under a planting cell that cannot hold the sapling, or dirt and grass replaced
 * when Humus is the next soil to lay; a sapling planted. Cells outside the pattern are left alone
 * except to fell a tree that grew into them.
 */
public final class ArboretumLogic implements FarmLogic {

    /** Group anchors fall every this many rows and columns. */
    static final int GROUP_SPACING = 3;

    /** Single saplings fall every this many rows and columns. */
    static final int LATTICE_SPACING = 2;

    /** Stands in for the planted sapling when checking soil before any germling is chosen. */
    private static final BlockState REFERENCE_SAPLING = Blocks.OAK_SAPLING.defaultBlockState();

    private final Predicate<ItemStack> germlings;
    private final Predicate<ItemStack> soils;
    private final Predicate<ItemStack> requires2x2;
    private final Predicate<ItemStack> humus;

    public ArboretumLogic() {
        this(
                stack -> stack.is(LogisticsForestry.TAG.ARBORETUM_GERMLINGS),
                stack -> stack.is(LogisticsForestry.TAG.ARBORETUM_SOILS),
                stack -> stack.is(LogisticsForestry.TAG.REQUIRES_2X2),
                stack -> stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()));
    }

    /** Tags are not bound in unit tests, so the item sets are injectable. */
    ArboretumLogic(
            Predicate<ItemStack> germlings,
            Predicate<ItemStack> soils,
            Predicate<ItemStack> requires2x2,
            Predicate<ItemStack> humus) {
        this.germlings = germlings;
        this.soils = soils;
        this.requires2x2 = requires2x2;
        this.humus = humus;
    }

    @Override
    public boolean laysSoil() {
        return true;
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

    /** Without its area the plot is treated as a 1×1 area of its own. */
    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        return decide(world, WorkArea.inFront(plot.relative(Direction.SOUTH), Direction.NORTH, 1), plot, inputs);
    }

    @Override
    public PlotAction decide(FarmWorld world, WorkArea area, BlockPos plot, FarmInputs inputs) {
        FellPlan tree = TreeFeller.plan(world.treeView(), plot, TreeFeller.Limits.DEFAULT);
        if (tree.isFound()) {
            return new PlotAction.Harvest(tree.blocks());
        }

        int index = area.indexOf(plot);
        if (index < 0) {
            return PlotAction.NONE;
        }
        int row = index / area.size();
        int column = index % area.size();
        boolean lattice = isLatticePoint(row, column);
        boolean anchor = isGroupAnchor(area.size(), row, column);
        if (!lattice && !anchor) {
            return PlotAction.NONE;
        }

        if (!isOpen(world.state(plot))) {
            return PlotAction.NONE;
        }
        return plantOrPrepare(world, area, row, column, lattice, anchor, inputs);
    }

    // ==================== Pattern ====================

    static boolean isLatticePoint(int row, int column) {
        return row % LATTICE_SPACING == 0 && column % LATTICE_SPACING == 0;
    }

    static boolean isGroupAnchor(int size, int row, int column) {
        return row % GROUP_SPACING == 0 && column % GROUP_SPACING == 0 && row + 1 < size && column + 1 < size;
    }

    /** The anchor and the three cells completing its 2×2 group, anchor first. */
    static List<BlockPos> group(WorkArea area, int row, int column) {
        int size = area.size();
        return List.of(
                area.plot(row * size + column),
                area.plot(row * size + column + 1),
                area.plot((row + 1) * size + column),
                area.plot((row + 1) * size + column + 1));
    }

    // ==================== Plant and soil ====================

    private PlotAction plantOrPrepare(
            FarmWorld world, WorkArea area, int row, int column, boolean lattice, boolean anchor, FarmInputs inputs) {
        Planting planting = choosePlanting(world, area, row, column, lattice, anchor, inputs);
        if (planting == null) {
            return PlotAction.NONE;
        }
        if (!planting.germling().isEmpty()) {
            BlockState sapling = plantState(planting.germling(), planting.cells().get(0), world);
            BlockPos soil = soilToLay(world, planting.cells(), sapling != null ? sapling : REFERENCE_SAPLING, inputs);
            if (soil != null) {
                return new PlotAction.PlaceSoil(soil);
            }
        }
        return new PlotAction.Plant(planting.cells(), planting.germling());
    }

    private record Planting(List<BlockPos> cells, ItemStack germling) {}

    /**
     * What to plant at this plot, or {@code null} when nothing should go here. An empty germling
     * means the plot wants planting but nothing suitable is on hand.
     */
    @Nullable
    private Planting choosePlanting(
            FarmWorld world, WorkArea area, int row, int column, boolean lattice, boolean anchor, FarmInputs inputs) {
        List<ItemStack> available = inputs.germlings();
        BlockPos plot = area.plot(row * area.size() + column);

        ItemStack shortGroup = ItemStack.EMPTY;
        if (anchor) {
            List<BlockPos> cells = group(area, row, column);
            if (allOpen(world, cells)) {
                for (ItemStack kind : available) {
                    if (!requires2x2.test(kind)) {
                        continue;
                    }
                    if (kind.getCount() >= cells.size()) {
                        return new Planting(cells, kind);
                    }
                    if (shortGroup.isEmpty()) {
                        shortGroup = kind;
                    }
                }
            }
        }
        if (lattice) {
            for (ItemStack kind : available) {
                if (!requires2x2.test(kind)) {
                    return new Planting(List.of(plot), kind);
                }
            }
        }
        if (!shortGroup.isEmpty()) {
            // A group short of saplings: the farm reports it is out of germlings.
            return new Planting(group(area, row, column), shortGroup);
        }
        if (lattice && available.isEmpty()) {
            return new Planting(List.of(plot), ItemStack.EMPTY);
        }
        return null;
    }

    /** The soil cell to lay under {@code cells} first, or {@code null} when they are all ready. */
    @Nullable
    private BlockPos soilToLay(FarmWorld world, List<BlockPos> cells, BlockState sapling, FarmInputs inputs) {
        if (!inputs.hasSoil()) {
            return null;
        }
        for (BlockPos cell : cells) {
            if (!world.canSurvive(sapling, cell)) {
                return cell.below();
            }
        }
        if (humus.test(inputs.soil())) {
            for (BlockPos cell : cells) {
                BlockState ground = world.state(cell.below());
                if (ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK)) {
                    return cell.below();
                }
            }
        }
        return null;
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
