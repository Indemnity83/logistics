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

    public ChanceOutput {
        if (!Float.isFinite(chance) || chance < 0f) {
            throw new IllegalArgumentException("chance must be finite and non-negative, got " + chance);
        }
    }

    /** The amount always produced (the integer part of the chance). */
    public int guaranteedCount() {
        return (int) chance;
    }

    /** The most a single {@link #roll} can yield: the guaranteed count plus the possible fractional bonus. */
    public int maxCount() {
        return guaranteedCount() + (fractionalBonus() > 0f ? 1 : 0);
    }

    /** Rolls this output's yield: the guaranteed count plus the fractional bonus. */
    public int roll(RandomSource random) {
        float bonus = fractionalBonus();
        return guaranteedCount() + (bonus > 0f && random.nextFloat() < bonus ? 1 : 0);
    }

    /** The fractional part of the chance — the probability of one extra item beyond the guaranteed count. */
    private float fractionalBonus() {
        return chance - guaranteedCount();
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
