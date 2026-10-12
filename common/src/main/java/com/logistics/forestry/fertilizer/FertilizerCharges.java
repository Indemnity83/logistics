package com.logistics.forestry.fertilizer;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Growth charges a farm gains from consuming one fertilizer item. Each charge pays for one
 * bonemeal-equivalent growth step.
 */
public final class FertilizerCharges {

    private FertilizerCharges() {}

    /**
     * Charges one item of {@code stack} is worth, ignoring the stack's count: the configured value for
     * Fertilizer and Bone Meal, 0 for anything else (including an empty stack).
     */
    public static int charges(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(LogisticsForestry.ITEM.FERTILIZER)) {
            return LogisticsConfigHost.get(LogisticsForestry.CONFIG.FERTILIZER_CHARGES);
        }
        if (stack.is(Items.BONE_MEAL)) {
            return LogisticsConfigHost.get(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES);
        }
        return 0;
    }
}
