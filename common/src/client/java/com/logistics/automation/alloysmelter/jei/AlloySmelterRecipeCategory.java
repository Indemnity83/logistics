package com.logistics.automation.alloysmelter.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
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
 * JEI recipe category for the Alloy Smelter.
 * Displays two inputs combined into a primary result, plus an optional chance-based byproduct.
 */
public class AlloySmelterRecipeCategory implements IRecipeCategory<AlloySmelterRecipe> {

    public static final RecipeType<AlloySmelterRecipe> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "alloy_smelter", AlloySmelterRecipe.class);

    // Matches AlloySmelterScreen.TEXTURE
    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/alloy_smelter.png").toIdentifier();

    // Compact JEI layout: two inputs -> arrow -> primary output, with the byproduct beside it.
    // The filled progress arrow sprite lives at UV (199,35), 24x16, in the GUI texture.
    private static final int INPUT_A_X = 8, INPUT_B_X = 26, INPUT_Y = 9;
    private static final int ARROW_X = 48, ARROW_Y = 10;
    private static final int OUTPUT_X = 78, OUTPUT_Y = 9;
    private static final int BYPRODUCT_X = 100, BYPRODUCT_Y = 9;

    private static final int WIDTH = 122;
    private static final int HEIGHT = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public AlloySmelterRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsAutomation.BLOCK.ALLOY_SMELTER));
        this.arrow = guiHelper.createDrawable(TEXTURE, 199, 35, 24, 16);
    }

    @Override
    public RecipeType<AlloySmelterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.alloy_smelter");
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
    public void draw(AlloySmelterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloySmelterRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_A_X, INPUT_Y)
            .addIngredients(recipe.inputA());
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_B_X, INPUT_Y)
            .addIngredients(recipe.inputB());
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
                    tooltip.add(Component.translatable("jei.logistics.alloy_smelter.byproduct_chance", pctStr)));
            }
        });
    }
}
