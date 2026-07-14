package com.logistics.automation.refinery.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.refinery.RefineryRecipe;
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
 * JEI recipe category for the Refinery: an input fluid distilled into an output fluid, plus an optional
 * chance-based item byproduct.
 */
public class RefineryRecipeCategory implements IRecipeCategory<RefineryRecipe> {

    public static final IRecipeType<RefineryRecipe> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "refinery", RefineryRecipe.class);

    // Matches RefineryScreen.TEXTURE; the filled progress arrow sprite lives at UV (199,35), 24x16.
    private static final ResourceId TEXTURE =
        LogisticsMod.modId("textures/gui/automation/refinery.png");

    // Compact JEI layout: input fluid -> arrow -> output fluid, with the byproduct beside it.
    private static final int INPUT_X = 8, INPUT_Y = 9;
    private static final int ARROW_X = 30, ARROW_Y = 10;
    private static final int OUTPUT_X = 60, OUTPUT_Y = 9;
    private static final int BYPRODUCT_X = 82, BYPRODUCT_Y = 9;

    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;

    private final IDrawable icon;
    private final IDrawable arrow;

    public RefineryRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsAutomation.BLOCK.REFINERY));
        this.arrow = guiHelper.createDrawable(TEXTURE.toIdentifier(), 199, 35, 24, 16);
    }

    @Override
    public IRecipeType<RefineryRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.refinery");
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RefineryRecipe recipe, IFocusGroup focuses) {
        builder.addDrawable(arrow, ARROW_X, ARROW_Y);
    }

    // JEI's cross-platform addFluidStack is marked deprecated-for-removal, but it's the only
    // loader-agnostic fluid API on this version (the alternative is the Fabric-only JeiFluidIngredient).
    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RefineryRecipe recipe, IFocusGroup focuses) {
        long inputAmount = recipe.input().nativeAmount();
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
            .setFluidRenderer(inputAmount, false, 16, 16)
            .addFluidStack(recipe.input().fluid(), inputAmount, DataComponentPatch.EMPTY);

        long outputAmount = recipe.result().nativeAmount();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .setFluidRenderer(outputAmount, false, 16, 16)
            .addFluidStack(recipe.result().fluid(), outputAmount, DataComponentPatch.EMPTY);

        recipe.byproduct().ifPresent(bp -> {
            float pct = bp.chance() * 100f;
            String pctStr = pct == Math.rint(pct) ? String.valueOf((int) pct) : String.format("%.1f", pct);
            builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, BYPRODUCT_Y)
                .add(bp.stack(1))
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.logistics.refinery.byproduct_chance", pctStr)));
        });
    }
}
