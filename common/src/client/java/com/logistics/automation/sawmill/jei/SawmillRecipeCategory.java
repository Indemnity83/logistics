package com.logistics.automation.sawmill.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.sawmill.SawmillRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Sawmill.
 * Displays an input ingredient cut into a primary product, plus a chance-based byproduct.
 */
public class SawmillRecipeCategory implements IRecipeCategory<SawmillRecipe> {

    public static final RecipeType<SawmillRecipe> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "sawmill", SawmillRecipe.class);

    // Matches SawmillScreen.TEXTURE
    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/sawmill.png").toIdentifier();

    // Compact JEI layout: input -> arrow -> primary output, with the byproduct beside it.
    // The filled progress arrow sprite lives at UV (199,35), 24x16, in the GUI texture (see SawmillScreen).
    private static final int INPUT_X = 8, INPUT_Y = 9;
    private static final int ARROW_X = 30, ARROW_Y = 10;
    private static final int OUTPUT_X = 60, OUTPUT_Y = 9;
    private static final int BYPRODUCT_X = 82, BYPRODUCT_Y = 9;

    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public SawmillRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsAutomation.BLOCK.SAWMILL));
        this.arrow = guiHelper.createDrawable(TEXTURE, 199, 35, 24, 16);
    }

    @Override
    public RecipeType<SawmillRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.sawmill");
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
    public void draw(SawmillRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SawmillRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
            .addIngredients(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .addItemStack(recipe.getResultItem());

        ItemStack byproduct = recipe.byproduct().stack(1);
        if (!byproduct.isEmpty()) {
            float pct = recipe.byproduct().chance() * 100f;
            String pctStr = pct == Math.rint(pct) ? String.valueOf((int) pct) : String.format("%.1f", pct);
            builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, BYPRODUCT_Y)
                .addItemStack(byproduct)
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.logistics.sawmill.byproduct_chance", pctStr)));
        }
    }
}
