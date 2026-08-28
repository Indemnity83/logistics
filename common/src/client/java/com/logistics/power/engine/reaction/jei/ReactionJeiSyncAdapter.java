package com.logistics.power.engine.reaction.jei;

import com.logistics.core.lib.jei.JeiRecipeSyncAdapter;
import com.logistics.core.lib.jei.MachineRecipeJeiSync;
import com.logistics.power.engine.reaction.ReactionRecipe;
import com.logistics.power.engine.reaction.ReactionRecipeSyncPacket;
import java.util.List;
import mezz.jei.api.recipe.IRecipeManager;

/** Owns the client-side Reaction Engine recipe cache and its JEI bridge. */
public final class ReactionJeiSyncAdapter implements JeiRecipeSyncAdapter {

    public static final ReactionJeiSyncAdapter INSTANCE = new ReactionJeiSyncAdapter();

    private volatile List<ReactionRecipe> recipes = List.of();

    private ReactionJeiSyncAdapter() {}

    public void set(ReactionRecipeSyncPacket packet) {
        MachineRecipeJeiSync.hideFromJei(this);
        recipes = List.copyOf(packet.recipes());
        MachineRecipeJeiSync.pushToJei(this);
    }

    public List<ReactionRecipe> recipes() {
        return recipes;
    }

    @Override
    public void pushToJei(IRecipeManager recipeManager) {
        recipeManager.addRecipes(ReactionRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void clear() {
        MachineRecipeJeiSync.hideFromJei(this);
        recipes = List.of();
    }

    @Override
    public void hideFromJei(IRecipeManager recipeManager) {
        recipeManager.hideRecipes(ReactionRecipeCategory.RECIPE_TYPE, recipes);
    }
}
