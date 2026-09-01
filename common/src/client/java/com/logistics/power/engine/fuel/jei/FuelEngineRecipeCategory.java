package com.logistics.power.engine.fuel.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.power.engine.fuel.FuelEngineFuels;
import com.logistics.power.engine.fuel.FuelEngineProfile;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * JEI recipe category for the Fuel Engine — one entry per supported fuel fluid.
 *
 * <p>Displays only figures from {@link FuelEngineFuels}' fixed table and the fixed batch sizes, never from
 * config — config is server-authoritative but unsynced, so a config-derived figure would show the viewing
 * client's own value.
 */
public class FuelEngineRecipeCategory implements IRecipeCategory<FuelEngineFuels.Entry> {

    public static final IRecipeType<FuelEngineFuels.Entry> RECIPE_TYPE =
        IRecipeType.create(LogisticsMod.MOD_ID, "fuel_engine", FuelEngineFuels.Entry.class);

    private static final int FUEL_X = 8, COOLANT_X = 40, Y = 9;
    private static final int WIDTH = 72;
    private static final int HEIGHT = 34;

    private final IDrawable icon;

    public FuelEngineRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LogisticsPower.BLOCK.FUEL_ENGINE));
    }

    @Override
    public IRecipeType<FuelEngineFuels.Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.power.fuel_engine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelEngineFuels.Entry entry, IFocusGroup focuses) {
        long fuelAmount = FluidUnits.mb(FuelEngineProfile.DEFAULT_FUEL_BATCH_MB);
        long coolantAmount = FluidUnits.mb(FuelEngineProfile.DEFAULT_COOLANT_BATCH_MB);
        builder.addSlot(RecipeIngredientRole.INPUT, FUEL_X, Y)
            .setFluidRenderer(fuelAmount, false, 16, 16)
            .addFluidStack(entry.fluid(), fuelAmount, DataComponentPatch.EMPTY)
            .addRichTooltipCallback((view, tooltip) -> addDetails(tooltip, entry));
        builder.addSlot(RecipeIngredientRole.INPUT, COOLANT_X, Y)
            .setFluidRenderer(coolantAmount, false, 16, 16)
            .addFluidStack(Fluids.WATER, coolantAmount, DataComponentPatch.EMPTY)
            .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                Component.translatable("jei.logistics.fuel_engine.coolant", FuelEngineProfile.DEFAULT_COOLANT_BATCH_MB)));
    }

    private static void addDetails(ITooltipBuilder tooltip, FuelEngineFuels.Entry entry) {
        tooltip.add(Component.translatable("jei.logistics.fuel_engine.energy", entry.fuel().energyPerBucket()));
        tooltip.add(Component.translatable(
            "jei.logistics.fuel_engine.batch",
            FuelEngineProfile.DEFAULT_FUEL_BATCH_MB,
            entry.fuel().energyPerBatch(FuelEngineProfile.DEFAULT_FUEL_BATCH_MB)));
    }
}
