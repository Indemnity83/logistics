package com.logistics.automation.transposer.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.transposer.TransposerRecipe;
import com.logistics.core.lib.recipe.FluidResult;
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
 * JEI recipe category for the Transposer: an input item converted into a result item, plus one fluid
 * gauge shown on whichever side the recipe's signed fluid amount belongs — under the input item for a
 * Fill recipe (fluid drained from the tank), under the result item for an Empty recipe (fluid deposited).
 */
public class TransposerRecipeCategory implements IRecipeCategory<TransposerRecipe> {

    public static final IRecipeType<TransposerRecipe> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "transposer", TransposerRecipe.class);

    // Matches TransposerScreen.TEXTURE; the filled progress arrow sprite lives at UV (199,35), 24x16.
    private static final ResourceId TEXTURE =
        LogisticsMod.modId("textures/gui/automation/transposer.png");

    private static final int ITEM_IN_X = 8, ITEM_IN_Y = 9;
    private static final int ARROW_X = 34, ARROW_Y = 10;
    private static final int ITEM_OUT_X = 88, ITEM_OUT_Y = 9;
    private static final int BYPRODUCT_X = 116, BYPRODUCT_Y = 9;
    private static final int FLUID_Y = 29;

    private static final int WIDTH = 140;
    private static final int HEIGHT = 48;

    private final IDrawable icon;
    private final IDrawable arrow;

    public TransposerRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsAutomation.BLOCK.TRANSPOSER));
        this.arrow = guiHelper.createDrawable(TEXTURE.toIdentifier(), 199, 35, 24, 16);
    }

    @Override
    public IRecipeType<TransposerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.transposer");
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, TransposerRecipe recipe, IFocusGroup focuses) {
        builder.addDrawable(arrow, ARROW_X, ARROW_Y);
    }

    // JEI's cross-platform addFluidStack is marked deprecated-for-removal, but it's the only
    // loader-agnostic fluid API on this version (the alternative is the Fabric-only JeiFluidIngredient).
    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TransposerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_IN_X, ITEM_IN_Y)
            .add(recipe.input());

        builder.addSlot(RecipeIngredientRole.OUTPUT, ITEM_OUT_X, ITEM_OUT_Y)
            .add(recipe.result().toStack());

        boolean fluidIsInput = recipe.fluid().isInput();
        FluidResult fluid = recipe.fluid().toFluidResult();
        long amount = fluid.nativeAmount();
        int fluidX = fluidIsInput ? ITEM_IN_X : ITEM_OUT_X;
        builder.addSlot(fluidIsInput ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, fluidX, FLUID_Y)
            .setFluidRenderer(amount, false, 16, 16)
            .addFluidStack(fluid.fluid(), amount, DataComponentPatch.EMPTY);

        recipe.byproduct().ifPresent(bp -> {
            float pct = bp.chance() * 100f;
            String pctStr = pct == Math.rint(pct) ? String.valueOf((int) pct) : String.format("%.1f", pct);
            builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, BYPRODUCT_Y)
                .add(bp.stack(1))
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.logistics.transposer.byproduct_chance", pctStr)));
        });
    }
}
