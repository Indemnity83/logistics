package com.logistics.power.engine.jei;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Puts the Stirling and Steam engines on JEI's vanilla fueling category.
 *
 * <p>Neither engine gets a category of its own on purpose. Both burn any vanilla furnace fuel, and neither
 * derives its output from which fuel is burned — the Stirling holds a target temperature under PID control
 * and the Steam engine's output falls out of its boiler simulation. A dedicated category would therefore
 * list hundreds of vanilla items against an identical output figure, restating the fueling category JEI
 * already ships. Registering the engines as catalysts instead means looking one up shows every fuel it
 * accepts, which is the question a player actually has.
 */
@JeiPlugin
public class EngineFuelJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_engine_fuel_plugin");

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        LOGGER.info("Registering stirling and steam engines on the vanilla fueling category");
        registration.addCraftingStation(RecipeTypes.SMELTING_FUEL, LogisticsPower.BLOCK.STIRLING_ENGINE);
        registration.addCraftingStation(RecipeTypes.SMELTING_FUEL, LogisticsPower.BLOCK.STEAM_ENGINE);
    }
}
