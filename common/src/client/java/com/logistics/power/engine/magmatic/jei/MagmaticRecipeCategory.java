package com.logistics.power.engine.magmatic.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI recipe category for the Magmatic Engine — lava in, power out, with the heat-dependent output range. */
public class MagmaticRecipeCategory implements IRecipeCategory<MagmaticFuelDisplay> {

    public static final RecipeType<MagmaticFuelDisplay> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "magmatic", MagmaticFuelDisplay.class);

    private static final int FLUID_X = 8, Y = 9;
    private static final int WIDTH = 72;
    private static final int HEIGHT = 34;

    private final IDrawable icon;

    public MagmaticRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsPower.BLOCK.MAGMATIC_ENGINE));
    }

    @Override
    public RecipeType<MagmaticFuelDisplay> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.power.magmatic_engine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MagmaticFuelDisplay fuel, IFocusGroup focuses) {
        long amount = FluidUnits.mb(fuel.batchMb());
        builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, Y)
            .setFluidRenderer(amount, false, 16, 16)
            .addFluidStack(fuel.fluid(), amount, DataComponentPatch.EMPTY)
            .addRichTooltipCallback((view, tooltip) -> addDetails(tooltip, fuel));
    }

    private static void addDetails(ITooltipBuilder tooltip, MagmaticFuelDisplay fuel) {
        tooltip.add(Component.translatable("jei.logistics.magmatic.batch", fuel.batchMb(), fuel.batchBurnTicks()));
        tooltip.add(Component.translatable("jei.logistics.magmatic.output", fuel.coldRf(), fuel.hotRf()));
        tooltip.add(Component.translatable("jei.logistics.magmatic.warm", fuel.warmRf()));
    }
}
