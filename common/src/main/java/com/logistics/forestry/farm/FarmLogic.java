package com.logistics.forestry.farm;

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
 * water, germlings, soil, fertilizer charges, a full output buffer, redstone and claim protection.
 *
 * <p>{@link #usesWater()} and {@link #laysSoil()} shape the machine (its tank and soil slots), so
 * they must never change for a given farm type.
 */
public interface FarmLogic {

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
     * {@code plot.below()}. Expected priority: harvest, then soil or tilling, then planting, then
     * irrigation (only while {@link FarmInputs#hasWater()}), then fertilizing.
     *
     * <p>Only called while {@code plot} is loaded. Must not change the world; reads beyond the plot
     * should check {@link FarmWorld#isLoaded}.
     */
    PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs);
}
