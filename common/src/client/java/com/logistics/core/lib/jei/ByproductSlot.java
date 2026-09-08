package com.logistics.core.lib.jei;

import com.logistics.core.machine.component.ChanceOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;

/**
 * Renders a machine's chance-based byproduct as a JEI output slot.
 *
 * <p>A byproduct's chance doubles as its count (see {@link ChanceOutput}), so the slot shows the
 * guaranteed yield and the tooltip reports only the fractional remainder — a chance of 1.25 is one
 * guaranteed plus a 25% chance of a second, not a "125% chance" of one. A whole chance gets no
 * tooltip at all.
 */
public final class ByproductSlot {

    private ByproductSlot() {}

    /**
     * Adds the byproduct slot at the given position, tooltipped with {@code chanceTranslationKey}
     * (a format string taking the bonus percentage) when there is a fractional bonus to report.
     */
    public static void add(
            IRecipeLayoutBuilder builder, int x, int y, ChanceOutput byproduct, String chanceTranslationKey) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
            .addItemStack(byproduct.displayStack());

        float bonus = byproduct.fractionalBonus();
        if (bonus > 0f) {
            String percent = formatPercent(bonus * 100f);
            slot.addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable(chanceTranslationKey, percent)));
        }
    }

    /** Whole percentages render without a decimal point; everything else keeps one place. */
    private static String formatPercent(float percent) {
        return percent == Math.rint(percent) ? String.valueOf((int) percent) : String.format("%.1f", percent);
    }
}
