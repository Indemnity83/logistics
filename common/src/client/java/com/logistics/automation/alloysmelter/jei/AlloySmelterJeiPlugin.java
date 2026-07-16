package com.logistics.automation.alloysmelter.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
import com.logistics.automation.jei.ClientMachineRecipes;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JEI plugin that registers the Alloy Smelter recipe category and all alloy-smelter recipes.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 */
@JeiPlugin
public class AlloySmelterJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");
    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_alloy_smelter_plugin");

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering alloy smelter recipe category");
        registration.addRecipeCategories(
            new AlloySmelterRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Recipes come from the server-synced client cache, so JEI works in singleplayer and multiplayer.
        List<AlloySmelterRecipe> recipes = ClientMachineRecipes.alloySmelter();
        LOGGER.info("Registering {} alloy smelter recipes with JEI", recipes.size());
        registration.addRecipes(AlloySmelterRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            LogisticsAutomation.BLOCK.ALLOY_SMELTER, AlloySmelterRecipeCategory.RECIPE_TYPE);
    }
}
