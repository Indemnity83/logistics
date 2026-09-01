package com.logistics.power.engine.fuel.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.fuel.FuelEngineFuels;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the Fuel Engine recipe category with JEI. */
@JeiPlugin
public class FuelEngineJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_fuel_engine_plugin");

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering fuel engine recipe category");
        registration.addRecipeCategories(
            new FuelEngineRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var fuels = FuelEngineFuels.entries();
        LOGGER.info("Registering {} fuel engine fuels with JEI", fuels.size());
        registration.addRecipes(FuelEngineRecipeCategory.RECIPE_TYPE, fuels);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            FuelEngineRecipeCategory.RECIPE_TYPE, LogisticsPower.BLOCK.FUEL_ENGINE);
    }
}
