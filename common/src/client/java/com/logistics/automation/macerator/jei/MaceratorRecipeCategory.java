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
 * Displays a single input ingredient grinding into an output item.
 */
public class MaceratorRecipeCategory implements IRecipeCategory<MaceratorRecipeWrapper> {

    public static final RecipeType<MaceratorRecipeWrapper> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "macerator", MaceratorRecipeWrapper.class);

    // Matches MaceratorScreen.TEXTURE
    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/core/macerator.png").toIdentifier();

    // GUI slot positions relative to background UV origin (48, 27).
    //   input: (8,8), output: (68,8). Arrow source: UV (180,36) size 25x14 at offset (32,9).
    private static final int INPUT_X = 8, INPUT_Y = 8;
    private static final int OUTPUT_X = 68, OUTPUT_Y = 8;
    private static final int ARROW_X = 32, ARROW_Y = 9;

    private static final int WIDTH = 86;
    private static final int HEIGHT = 26;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public MaceratorRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 48, 27, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsAutomation.BLOCK.MACERATOR));
        this.arrow = guiHelper.createDrawable(TEXTURE, 180, 36, 25, 14);
    }

    @Override
    public RecipeType<MaceratorRecipeWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.core.macerator");
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
    }
}
