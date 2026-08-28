package com.logistics.automation.jei;

import com.logistics.automation.alloysmelter.jei.AlloySmelterRecipeCategory;
import com.logistics.automation.crucible.jei.CrucibleRecipeCategory;
import com.logistics.automation.fabricator.jei.FabricatorRecipeCategory;
import com.logistics.automation.macerator.jei.MaceratorRecipeCategory;
import com.logistics.automation.refinery.jei.RefineryRecipeCategory;
import com.logistics.automation.sawmill.jei.SawmillRecipeCategory;
import com.logistics.automation.transposer.jei.TransposerRecipeCategory;
import com.logistics.core.lib.jei.JeiRecipeSyncAdapter;
import mezz.jei.api.recipe.IRecipeManager;

/** Publishes the automation domain's synchronized recipes to JEI. */
public final class AutomationJeiSyncAdapter implements JeiRecipeSyncAdapter {

    public static final AutomationJeiSyncAdapter INSTANCE = new AutomationJeiSyncAdapter();

    private AutomationJeiSyncAdapter() {}

    @Override
    public void pushToJei(IRecipeManager recipeManager) {
        recipeManager.addRecipes(MaceratorRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.macerator());
        recipeManager.addRecipes(SawmillRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.sawmill());
        recipeManager.addRecipes(CrucibleRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.crucible());
        recipeManager.addRecipes(AlloySmelterRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.alloySmelter());
        recipeManager.addRecipes(RefineryRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.refinery());
        recipeManager.addRecipes(FabricatorRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.fabricator());
        recipeManager.addRecipes(TransposerRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.transposer());
    }
}
