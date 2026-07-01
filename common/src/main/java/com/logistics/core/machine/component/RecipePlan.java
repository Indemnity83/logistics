package com.logistics.core.machine.component;

import com.logistics.core.lib.recipe.FluidResult;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A resolved recipe in RF-cost terms: the total energy the machine must spend, how many input items
 * each input slot consumes per run, the primary output, any chance-based byproducts, an experience
 * reward, and an optional fluid output. Resolvers translate a machine's current inputs (custom or
 * vanilla recipes) into this executable form.
 *
 * <p>{@code inputCounts} carries one entry per input slot in slot order; single-input machines pass a
 * length-1 array via the convenience constructors. {@code fluidResult} is {@code null} for item-only
 * machines, so their behaviour is unchanged.
 */
public record RecipePlan(
        long energyRequired,
        int[] inputCounts,
        ItemStack result,
        List<ChanceOutput> byproducts,
        float experience,
        @Nullable FluidResult fluidResult) {

    public RecipePlan {
        // Own the array (callers reuse buffers) and reject negative counts — a negative reaches
        // ItemStack.shrink as a grow.
        inputCounts = inputCounts.clone();
        for (int count : inputCounts) {
            if (count < 0) {
                throw new IllegalArgumentException("input counts must be non-negative, got " + count);
            }
        }
    }

    /** Item-output recipe with explicit input counts and no fluid output. */
    public RecipePlan(
            long energyRequired, int[] inputCounts, ItemStack result, List<ChanceOutput> byproducts, float experience) {
        this(energyRequired, inputCounts, result, byproducts, experience, null);
    }

    /** Single-input, single-output recipe with no byproducts (the macerator/kiln case). */
    public RecipePlan(long energyRequired, ItemStack result, float experience) {
        this(energyRequired, new int[] {1}, result, List.of(), experience, null);
    }

    /** Single-input recipe with an explicit input count and byproducts (the sawmill case). */
    public RecipePlan(
            long energyRequired, int inputCount, ItemStack result, List<ChanceOutput> byproducts, float experience) {
        this(energyRequired, new int[] {inputCount}, result, byproducts, experience, null);
    }

    /** Single-input, fluid-only output recipe (the magma crucible): no item result or byproducts. */
    public RecipePlan(long energyRequired, int inputCount, FluidResult fluidResult, float experience) {
        this(energyRequired, new int[] {inputCount}, ItemStack.EMPTY, List.of(), experience, fluidResult);
    }

    /** The count consumed from the primary input slot. */
    public int inputCount() {
        return inputCounts.length > 0 ? inputCounts[0] : 0;
    }
}
