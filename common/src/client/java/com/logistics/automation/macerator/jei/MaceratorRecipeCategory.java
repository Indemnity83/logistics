package com.logistics.automation.macerator.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Macerator.
 * Displays an input ingredient ground into a primary dust, plus a chance-based byproduct (ore recipes).
 */
public class MaceratorRecipeCategory implements IRecipeCategory<MaceratorRecipeWrapper> {

    public static final RecipeType<MaceratorRecipeWrapper> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "macerator", MaceratorRecipeWrapper.class);

    // Matches MaceratorScreen.TEXTURE
    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/macerator.png").toIdentifier();

    // Compact JEI layout: input -> arrow -> primary output, with the byproduct beside it.
    // The filled progress arrow sprite lives at UV (199,35), 24x16, in the GUI texture (see MaceratorScreen).
    private static final int INPUT_X = 8, INPUT_Y = 9;
    private static final int ARROW_X = 30, ARROW_Y = 10;
    private static final int OUTPUT_X = 60, OUTPUT_Y = 9;
    private static final int BYPRODUCT_X = 82, BYPRODUCT_Y = 9;

    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public MaceratorRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 48, 27, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsAutomation.BLOCK.MACERATOR));
        this.arrow = guiHelper.createDrawable(TEXTURE, 199, 35, 24, 16);
    }

    @Override
    public RecipeType<MaceratorRecipeWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.macerator");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(MaceratorRecipeWrapper recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MaceratorRecipeWrapper recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
            .addIngredients(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .addItemStack(recipe.getResultItem());

        recipe.byproduct().ifPresent(bp -> {
            // chance doubles as count: floor(chance) is guaranteed, the remainder is a bonus chance.
            int guaranteed = (int) bp.chance();
            float bonus = bp.chance() - guaranteed;
            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, BYPRODUCT_Y)
                .addItemStack(bp.stack(Math.max(1, guaranteed)));
            if (bonus > 0f) {
                float pct = bonus * 100f;
                String pctStr = pct == Math.rint(pct) ? String.valueOf((int) pct) : String.format("%.1f", pct);
                slot.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.logistics.macerator.byproduct_chance", pctStr)));
            }
        });
    }
}
