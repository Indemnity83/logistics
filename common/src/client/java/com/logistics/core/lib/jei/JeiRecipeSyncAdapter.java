package com.logistics.core.lib.jei;

import mezz.jei.api.recipe.IRecipeManager;

/** Supplies late-arriving recipes to JEI without coupling the shared cache to a domain. */
public interface JeiRecipeSyncAdapter {

    void pushToJei(IRecipeManager recipeManager);

    default void hideFromJei(IRecipeManager recipeManager) {}

    default void clear() {}
}
