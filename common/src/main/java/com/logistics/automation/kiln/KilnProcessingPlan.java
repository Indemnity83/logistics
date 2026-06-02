package com.logistics.automation.kiln;

import net.minecraft.world.item.ItemStack;

final class KilnProcessingPlan {
    private KilnProcessingPlan() {}

    static Result advance(int progress, int cookingTime, long energyStored, int energyPerTick, boolean outputAccepted) {
        if (energyStored < energyPerTick || !outputAccepted) {
            return new Result(progress, false, false, false);
        }

        int nextProgress = progress + 1;
        return new Result(nextProgress, true, true, nextProgress >= cookingTime);
    }

    static boolean acceptsOutput(ItemStack output, ItemStack result) {
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    record Result(int progress, boolean consumedEnergy, boolean lit, boolean complete) {}
}
