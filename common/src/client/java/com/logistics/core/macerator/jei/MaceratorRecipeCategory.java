package com.logistics.core.macerator.jei;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.core.macerator.MaceratorRecipeWrapper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Macerator.
 * Displays a single input ingredient grinding into an output item.
 */
public class MaceratorRecipeCategory implements IRecipeCategory<MaceratorRecipeWrapper> {

    public static final IRecipeType<MaceratorRecipeWrapper> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "macerator", MaceratorRecipeWrapper.class);

    // Matches MaceratorScreen.TEXTURE
    private static final ResourceId TEXTURE =
        LogisticsMod.modId("textures/gui/core/macerator.png");

    // GUI slot positions: input at (56,35), output at (116,35).
    // Background region origin: UV (48,27). Relative positions:
    //   input: (8,8), output: (68,8). Arrow source: UV (180,36) size 25x14 at offset (32,9).
    private static final int INPUT_X = 8, INPUT_Y = 8;
    private static final int OUTPUT_X = 68, OUTPUT_Y = 8;
    private static final int ARROW_X = 32, ARROW_Y = 9;

    private static final int WIDTH = 86;
    private static final int HEIGHT = 26;

    private final IDrawable icon;
    private final IDrawable arrow;

    public MaceratorRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsCore.BLOCK.MACERATOR));
        this.arrow = guiHelper.createDrawable(TEXTURE.toIdentifier(), 180, 36, 25, 14);
    }

    @Override
    public IRecipeType<MaceratorRecipeWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.core.macerator");
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MaceratorRecipeWrapper recipe, IFocusGroup focuses) {
        builder.addDrawable(arrow, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MaceratorRecipeWrapper recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
            .add(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .add(recipe.getResultItem());
    }
}
