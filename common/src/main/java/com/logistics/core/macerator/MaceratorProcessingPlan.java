package com.logistics.core.macerator;

import net.minecraft.world.item.ItemStack;

/**
 * Pure processing math for the Macerator, extracted so it can be unit-tested without a live
 * {@code Level}. Mirrors {@code KilnProcessingPlan} in the automation domain.
 */
final class MaceratorProcessingPlan {
    private MaceratorProcessingPlan() {}

    static Result advance(int progress, int grindingTime, long energyStored, int energyPerTick, boolean outputAccepted) {
        if (energyStored < energyPerTick || !outputAccepted) {
            return new Result(progress, false, false, false);
        }

        int nextProgress = progress + 1;
        return new Result(nextProgress, true, true, nextProgress >= grindingTime);
    }

    static boolean acceptsOutput(ItemStack output, ItemStack result) {
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    record Result(int progress, boolean consumedEnergy, boolean lit, boolean complete) {}
}
