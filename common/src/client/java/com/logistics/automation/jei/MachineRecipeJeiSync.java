package com.logistics.automation.jei;

import com.logistics.automation.alloysmelter.jei.AlloySmelterRecipeCategory;
import com.logistics.automation.crucible.jei.CrucibleRecipeCategory;
import com.logistics.automation.fabricator.jei.FabricatorRecipeCategory;
import com.logistics.automation.macerator.jei.MaceratorRecipeCategory;
import com.logistics.automation.refinery.jei.RefineryRecipeCategory;
import com.logistics.automation.sawmill.jei.SawmillRecipeCategory;
import com.logistics.automation.transposer.jei.TransposerRecipeCategory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * Bridges late-arriving synced recipes into an already-loaded JEI. On some loaders (notably NeoForge)
 * the server's recipe packet lands after JEI has run {@code registerRecipes} against an empty cache,
 * so once the runtime is live we push the freshly-synced recipes straight in. Pushing only on packet
 * arrival (never on runtime-available) keeps it dup-free: in any JEI load cycle exactly one of
 * {@code registerRecipes} or this push supplies the recipes.
 */
public final class MachineRecipeJeiSync {

    private static volatile IJeiRuntime runtime;

    private MachineRecipeJeiSync() {}

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void pushToJei() {
        IJeiRuntime current = runtime;
        if (current == null) {
            return;
        }
        IRecipeManager recipes = current.getRecipeManager();
        recipes.addRecipes(MaceratorRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.macerator());
        recipes.addRecipes(SawmillRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.sawmill());
        recipes.addRecipes(CrucibleRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.crucible());
        recipes.addRecipes(AlloySmelterRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.alloySmelter());
        recipes.addRecipes(RefineryRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.refinery());
        recipes.addRecipes(FabricatorRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.fabricator());
        recipes.addRecipes(TransposerRecipeCategory.RECIPE_TYPE, ClientMachineRecipes.transposer());
    }
}
