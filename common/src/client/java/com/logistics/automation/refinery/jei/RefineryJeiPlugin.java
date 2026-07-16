package com.logistics.automation.refinery.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.refinery.RefineryRecipe;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
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
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
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
        // Recipe data only exists on the (integrated) server, so JEI shows refinery recipes in singleplayer.
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        List<RefineryRecipe> recipes = server.getRecipeManager().getRecipes().stream()
            .map(RecipeHolder::value)
            .filter(RefineryRecipe.class::isInstance)
            .map(RefineryRecipe.class::cast)
            .toList();
        LOGGER.info("Registering {} refinery recipes with JEI", recipes.size());
        registration.addRecipes(RefineryRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            LogisticsAutomation.BLOCK.REFINERY, RefineryRecipeCategory.RECIPE_TYPE);
    }
}
