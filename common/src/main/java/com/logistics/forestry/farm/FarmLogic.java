package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What sets one farm type apart: what it plants, what soil it lays, and what each plot needs next.
 *
 * <p>One stateless instance per farm block ({@link FarmMachineBlock#logic()}), shared by every farm
 * of that type. A logic only reads the world, through {@link FarmWorld}, and answers with a single
 * {@link PlotAction}; the {@link FarmComponent} carries it out and owns the farm-wide gates: power,
 * water, germlings, soil, a full output buffer, redstone and claim protection.
 *
 * <p>{@link #usesWater()} and {@link #laysSoil()} shape the machine (its tank and soil slots), so
 * they must never change for a given farm type.
 *
 * <p>A farm works the ring of plots between two squares centred on it, {@link #ringInner()} and
 * {@link #ringOuter()} wide. Before the logic is asked about a plot, the farm clears every block
 * from the plot's column, {@link #clearanceHeight()} tall, that is not {@link #isOwnGrowth own
 * growth}; the logic only decides once that column is clear.
 */
public interface FarmLogic {

    /** Side of the square around the farm, the farm included, that is never worked (odd). */
    default int ringInner() {
        return 9;
    }

    /** Side of the square whose ring outside {@link #ringInner()} is worked (odd, larger). */
    default int ringOuter() {
        return 13;
    }

    /** Blocks the farm keeps clear above each plot, counting the plot itself. */
    default int clearanceHeight() {
        return 1;
    }

    /** Whether {@code state} is the farm's own growth, which it harvests instead of clearing. */
    default boolean isOwnGrowth(BlockState state) {
        return false;
    }

    /** Whether the farm has an input-only water tank and irrigates. */
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
     * The one thing {@code plot} needs next. {@code plot} is at the farm's Y level and its soil is
     * {@code plot.below()}; above the soil the column holds only air and own growth. Expected
     * priority: harvest, then soil or tilling, then planting, then irrigation (only while
     * {@link FarmInputs#hasWater()}).
     *
     * <p>Only called while {@code plot} is loaded. Must not change the world; reads beyond the plot
     * should check {@link FarmWorld#isLoaded}.
     */
    PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs);

    /**
     * {@link #decide(FarmWorld, BlockPos, FarmInputs)} for a plot of {@code area}; the farm calls
     * this one. Override it when the planting pattern depends on where {@code plot} lies in the
     * area. Defaults to ignoring the area.
     */
    default PlotAction decide(FarmWorld world, PlotArea area, BlockPos plot, FarmInputs inputs) {
        return decide(world, plot, inputs);
    }
}
