package com.logistics.core.macerator.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.core.macerator.MaceratorRecipeWrapper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
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
        // Full recipe data only exists on the (integrated) server; clients are not sent the
        // recipe list in modern Minecraft, so JEI shows macerator recipes in singleplayer.
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        List<MaceratorRecipeWrapper> recipes = server.getRecipeManager().getRecipes().stream()
            .map(RecipeHolder::value)
            .filter(MaceratorRecipeWrapper.class::isInstance)
            .map(MaceratorRecipeWrapper.class::cast)
            .toList();
        LOGGER.info("Registering {} macerator recipes with JEI", recipes.size());
        registration.addRecipes(MaceratorRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            MaceratorRecipeCategory.RECIPE_TYPE, LogisticsAutomation.BLOCK.MACERATOR);
    }
}
