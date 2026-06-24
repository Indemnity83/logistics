package com.logistics.core.machine.component;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * A resolved recipe in RF-cost terms: the total energy the machine must spend, how many input items
 * one run consumes, the primary output, any chance-based byproducts, and an experience reward.
 * Resolvers translate a machine's current inputs (custom or vanilla recipes) into this executable
 * form.
 */
public record RecipePlan(
        long energyRequired, int inputCount, ItemStack result, List<ChanceOutput> byproducts, float experience) {

    /** Single-input, single-output recipe with no byproducts (the macerator/kiln case). */
    public RecipePlan(long energyRequired, ItemStack result, float experience) {
        this(energyRequired, 1, result, List.of(), experience);
    }
}
