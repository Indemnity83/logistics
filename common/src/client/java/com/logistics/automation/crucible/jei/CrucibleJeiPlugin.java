package com.logistics.automation.crucible.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.crucible.CrucibleRecipe;
import com.logistics.automation.jei.ClientMachineRecipes;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JEI plugin that registers the Crucible recipe category and all crucible recipes. Discovered by JEI via
 * the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 */
@JeiPlugin
public class CrucibleJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");
    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_crucible_plugin");

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering crucible recipe category");
        registration.addRecipeCategories(
            new CrucibleRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Recipes come from the server-synced client cache, so JEI works in singleplayer and multiplayer.
        List<CrucibleRecipe> recipes = ClientMachineRecipes.crucible();
        LOGGER.info("Registering {} crucible recipes with JEI", recipes.size());
        registration.addRecipes(CrucibleRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            LogisticsAutomation.BLOCK.CRUCIBLE, CrucibleRecipeCategory.RECIPE_TYPE);
    }
}
