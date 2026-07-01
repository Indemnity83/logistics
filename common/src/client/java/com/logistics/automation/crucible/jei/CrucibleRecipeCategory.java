package com.logistics.automation.crucible.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.crucible.CrucibleRecipe;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Crucible: an input item (or list of items) melted into a fluid output.
 */
public class CrucibleRecipeCategory implements IRecipeCategory<CrucibleRecipe> {

    public static final IRecipeType<CrucibleRecipe> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "crucible", CrucibleRecipe.class);

    // Matches CrucibleScreen.TEXTURE (progress arrow sprite at UV 199,35, 24x16).
    private static final ResourceId TEXTURE =
        LogisticsMod.modId("textures/gui/automation/crucible.png");

    private static final int INPUT_X = 8, INPUT_Y = 9;
    private static final int ARROW_X = 30, ARROW_Y = 10;
    private static final int OUTPUT_X = 60, OUTPUT_Y = 9;

    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;

    private final IDrawable icon;
    private final IDrawable arrow;

    public CrucibleRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsAutomation.BLOCK.CRUCIBLE));
        this.arrow = guiHelper.createDrawable(TEXTURE.toIdentifier(), 199, 35, 24, 16);
    }

    @Override
    public IRecipeType<CrucibleRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.crucible");
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CrucibleRecipe recipe, IFocusGroup focuses) {
        builder.addDrawable(arrow, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
            .add(recipe.ingredient());

        // Native (platform) fluid amount so JEI reports the right mB in the tooltip; a matching renderer
        // capacity shows the fluid filled.
        long amount = recipe.result().nativeAmount();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .setFluidRenderer(amount, false, 16, 16)
            .addFluidStack(recipe.result().fluid(), amount, DataComponentPatch.EMPTY);
    }
}
