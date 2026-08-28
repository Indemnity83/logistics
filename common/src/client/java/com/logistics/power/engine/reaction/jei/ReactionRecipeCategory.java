package com.logistics.power.engine.reaction.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.power.engine.reaction.ReactionRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI recipe category for the energy-producing Reaction Engine. */
public class ReactionRecipeCategory implements IRecipeCategory<ReactionRecipe> {

    public static final IRecipeType<ReactionRecipe> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "reaction", ReactionRecipe.class);

    private static final int FLUID_X = 8, ITEM_X = 40, Y = 9;
    private static final int WIDTH = 72;
    private static final int HEIGHT = 34;

    private final IDrawable icon;

    public ReactionRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsPower.BLOCK.REACTION_ENGINE));
    }

    @Override
    public IRecipeType<ReactionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.power.reaction_engine");
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

    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactionRecipe recipe, IFocusGroup focuses) {
        long amount = recipe.reactant().nativeAmount();
        builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, Y)
            .setFluidRenderer(amount, false, 16, 16)
            .addFluidStack(recipe.reactant().fluid(), amount, DataComponentPatch.EMPTY)
            .addRichTooltipCallback((view, tooltip) -> addDetails(tooltip, recipe));
        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X, Y)
            .add(recipe.reagent())
            .addRichTooltipCallback((view, tooltip) -> addDetails(tooltip, recipe));
    }

    private static void addDetails(ITooltipBuilder tooltip, ReactionRecipe recipe) {
        tooltip.add(Component.translatable("jei.logistics.reaction.energy", recipe.energy()));
        tooltip.add(Component.translatable("jei.logistics.reaction.output", recipe.outputPerTick()));
        tooltip.add(Component.translatable("jei.logistics.reaction.time", recipe.time()));
        if (recipe.reagentCount() != 1) {
            tooltip.add(Component.translatable("jei.logistics.reaction.reagent_count", recipe.reagentCount()));
        }
    }
}
