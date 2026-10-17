package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotLayout;
import com.logistics.forestry.logic.LevelTreeView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What sets one farm type apart: what it plants, what soil it lays, and what each plot needs next.
 *
 * <p>One stateless instance per farm block ({@link FarmMachineBlock#logic()}), shared by every farm
 * of that type. A logic only reads the world, through {@link FarmWorld}, and answers with a single
 * {@link PlotAction}; the {@link FarmComponent} carries it out and owns the farm-wide gates: power,
 * germlings, soil, a full output buffer, redstone and claim protection, and the irrigation of its
 * ring's farmland.
 *
 * <p>{@link #usesWater()} and {@link #laysSoil()} shape the machine (its tank and soil slots), so
 * they must never change for a given farm type.
 *
 * <p>A farm works the ring of plots around its {@link FarmGeometry#COURTYARD}, out to its tier's
 * outer size, one block above the farm so that their soil is level with it. Before the logic is
 * asked about a plot, the farm clears every block from the plot's column,
 * {@link #clearanceHeight()} tall, that is not {@link #isOwnGrowth own growth}; the logic only
 * decides once that column is clear.
 *
 * <p>Contract, so that one farm may later split its ring between several logics ({@link FarmPlan}):
 *
 * <ul>
 *   <li>A logic is stateless; it decides only from the world, its region, the plot and its inputs.
 *   <li>Every per-plot question (deciding, clearance height, own growth, soil after harvest, which
 *       germlings and soil it accepts) is asked of the logic that works that plot.
 *   <li>Patterns use region-relative lane and step coordinates through {@link PlotLayout}, defined
 *       per lane from the inside out, so widening the ring only adds lanes. A logic never depends
 *       on how many plots the whole ring has.
 *   <li>Every cell of a multi-cell placement (a 2×2 tree group) satisfies {@code region.contains}.
 *   <li>Trees are planned through {@link FarmWorld#fellPlan}, never with {@code TreeFeller}
 *       directly, so the farm can spread a large tree's planning over its check budget.
 *   <li>Trees are felled within the region's bounds
 *       ({@link com.logistics.forestry.logic.TreeFeller.Limits#within}), never in
 *       its {@link PlotLayout#keepOut()}.
 * </ul>
 */
public interface FarmLogic {

    /** Blocks the farm keeps clear above each plot, counting the plot itself. */
    default int clearanceHeight() {
        return 1;
    }

    /** Whether {@code state} is the farm's own growth, which it harvests instead of clearing. */
    default boolean isOwnGrowth(BlockState state) {
        return false;
    }

    /**
     * Whether the farm has an input-only water tank, from which it irrigates its ring's farmland while
     * working; see {@link FarmComponent}.
     */
    default boolean usesWater() {
        return false;
    }

    /** Whether the farm lays soil from its soil slots; when false those slots accept nothing. */
    default boolean laysSoil() {
        return false;
    }

    /** Items the germling slots accept. */
    boolean isGermling(ItemStack stack);

    /** Items the soil slots accept. Only consulted when {@link #laysSoil()}. */
    default boolean isSoil(ItemStack stack) {
        return false;
    }

    /**
     * Whether planting takes matching germlings from the output slots (harvest drops such as seeds
     * and saplings) before the germling slots.
     */
    default boolean replantsFromOutput() {
        return false;
    }

    /**
     * The block state planting {@code germling} at {@code pos} places, or {@code null} when it cannot
     * be planted there. Defaults to the germling's block in its default state.
     */
    @Nullable
    default BlockState plantState(ItemStack germling, BlockPos pos, FarmWorld world) {
        return germling.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
    }

    /**
     * The one thing {@code plot} needs next. {@code plot} is one block above the farm and its soil,
     * {@code plot.below()}, is level with the farm; from the plot up, the column holds only air and
     * own growth. Expected priority: harvest, then soil or tilling, then planting.
     *
     * <p>Only called while {@code plot} is loaded. Must not change the world; reads beyond the plot
     * should check {@link FarmWorld#isLoaded}.
     */
    PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs);

    /**
     * The block a harvest leaves under its plot in place of {@code soil}, e.g. a spent soil back to
     * dirt; {@code null} leaves the soil as it is. Applied once a harvest has fully completed and the plot
     * stands empty.
     */
    @Nullable
    default BlockState soilAfterHarvest(BlockState soil) {
        return null;
    }

    /**
     * What harvesting {@code state} costs. Defaults to a crop for a {@link CropBlock}, a log for
     * {@code logistics:forestry/fellable_wood}, leaves for {@code logistics:forestry/fellable_canopy},
     * and a crop for anything else.
     */
    default FarmCost harvestCost(BlockState state) {
        if (state.getBlock() instanceof CropBlock) {
            return FarmCost.HARVEST_CROP;
        }
        if (state.is(LevelTreeView.FELLABLE_WOOD)) {
            return FarmCost.HARVEST_LOG;
        }
        if (state.is(LevelTreeView.FELLABLE_CANOPY)) {
            return FarmCost.HARVEST_LEAVES;
        }
        return FarmCost.HARVEST_CROP;
    }

    /**
     * {@link #decide(FarmWorld, BlockPos, FarmInputs)} for a plot of {@code region}; the farm calls
     * this one. Override it when the planting pattern depends on where {@code plot} lies in the
     * region. Defaults to ignoring the region.
     */
    default PlotAction decide(FarmWorld world, PlotLayout region, BlockPos plot, FarmInputs inputs) {
        return decide(world, plot, inputs);
    }
}
