package com.logistics.core.machine.component;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * A chance-based recipe output (a byproduct). The chance doubles as the count: {@code floor(chance)}
 * is always produced and the fractional remainder is the probability of one extra — so 1.0 yields
 * exactly one, and 1.25 yields one guaranteed plus a 25% chance of a second (Thermal-style secondary
 * outputs).
 */
public record ChanceOutput(ItemStack template, float chance) {

    /** The amount always produced (the integer part of the chance). */
    public int guaranteedCount() {
        return (int) chance;
    }

    /** Rolls this output's yield: the guaranteed count plus the fractional bonus. */
    public int roll(RandomSource random) {
        int guaranteed = (int) chance;
        float bonus = chance - guaranteed;
        return guaranteed + (bonus > 0f && random.nextFloat() < bonus ? 1 : 0);
    }

    /** A copy of the template with the given count. */
    public ItemStack stack(int count) {
        return template.copyWithCount(count);
    }

    /** The guaranteed portion as a stack (may be empty when the chance is purely fractional). */
    public ItemStack guaranteedStack() {
        return template.copyWithCount(guaranteedCount());
    }
}
