package com.logistics.automation.macerator.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.jei.ClientMachineRecipes;
import com.logistics.automation.jei.MachineRecipeJeiSync;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.logistics.core.lib.resource.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JEI plugin that registers the Macerator recipe category and all macerator recipes.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 */
@JeiPlugin
public class MaceratorJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");
    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_plugin");

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering macerator recipe category");
        registration.addRecipeCategories(
            new MaceratorRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Recipes come from the server-synced client cache, so JEI works in singleplayer and multiplayer.
        List<MaceratorRecipeWrapper> recipes = ClientMachineRecipes.macerator();
        LOGGER.info("Registering {} macerator recipes with JEI", recipes.size());
        registration.addRecipes(MaceratorRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            MaceratorRecipeCategory.RECIPE_TYPE, LogisticsAutomation.BLOCK.MACERATOR);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        MachineRecipeJeiSync.onRuntimeAvailable(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        MachineRecipeJeiSync.onRuntimeUnavailable();
    }
}
