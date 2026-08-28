package com.logistics.power.engine.reaction.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.reaction.ReactionRecipe;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

/** Registers the Reaction Engine recipe category with JEI. */
@JeiPlugin
public class ReactionJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_reaction_plugin");

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
            new ReactionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ReactionRecipe> recipes = ReactionJeiSyncAdapter.INSTANCE.recipes();
        registration.addRecipes(ReactionRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            LogisticsPower.BLOCK.REACTION_ENGINE, ReactionRecipeCategory.RECIPE_TYPE);
    }
}
