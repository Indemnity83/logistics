package com.logistics.core.machine.component;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * A resolved recipe in RF-cost terms: the total energy the machine must spend, how many input items
 * each input slot consumes per run, the primary output, any chance-based byproducts, and an
 * experience reward. Resolvers translate a machine's current inputs (custom or vanilla recipes) into
 * this executable form.
 *
 * <p>{@code inputCounts} carries one entry per input slot in slot order; single-input machines pass a
 * length-1 array via the convenience constructors.
 */
public record RecipePlan(
        long energyRequired, int[] inputCounts, ItemStack result, List<ChanceOutput> byproducts, float experience) {

    /** Single-input, single-output recipe with no byproducts (the macerator/kiln case). */
    public RecipePlan(long energyRequired, ItemStack result, float experience) {
        this(energyRequired, new int[] {1}, result, List.of(), experience);
    }

    /** Single-input recipe with an explicit input count and byproducts (the sawmill case). */
    public RecipePlan(
            long energyRequired, int inputCount, ItemStack result, List<ChanceOutput> byproducts, float experience) {
        this(energyRequired, new int[] {inputCount}, result, byproducts, experience);
    }

    /** The count consumed from the primary input slot. */
    public int inputCount() {
        return inputCounts.length > 0 ? inputCounts[0] : 0;
    }
}
