package com.logistics.automation.refinery.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.jei.ClientMachineRecipes;
import com.logistics.automation.refinery.RefineryRecipe;
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
 * JEI plugin that registers the Refinery recipe category and all refinery recipes. Discovered by JEI via
 * the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json and the {@link JeiPlugin} annotation on NeoForge.
 */
@JeiPlugin
public class RefineryJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");
    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_refinery_plugin");

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering refinery recipe category");
        registration.addRecipeCategories(
            new RefineryRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Recipes come from the server-synced client cache, so JEI works in singleplayer and multiplayer.
        List<RefineryRecipe> recipes = ClientMachineRecipes.refinery();
        LOGGER.info("Registering {} refinery recipes with JEI", recipes.size());
        registration.addRecipes(RefineryRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            RefineryRecipeCategory.RECIPE_TYPE, LogisticsAutomation.BLOCK.REFINERY);
    }
}
