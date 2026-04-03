package com.logistics.automation.macerator.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.macerator.MaceratorRecipeManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation PLUGIN_ID =
        LogisticsMod.modId("jei_plugin").toResourceLocation();

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering macerator recipe category");
        registration.addRecipeCategories(
            new MaceratorRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var recipes = List.copyOf(MaceratorRecipeManager.getAllRecipes().values());
        LOGGER.info("Registering {} macerator recipes with JEI", recipes.size());
        registration.addRecipes(MaceratorRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            MaceratorRecipeCategory.RECIPE_TYPE, LogisticsAutomation.BLOCK.MACERATOR);
    }
}
