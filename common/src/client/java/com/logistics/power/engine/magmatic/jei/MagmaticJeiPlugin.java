package com.logistics.power.engine.magmatic.jei;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsMod;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.magmatic.MagmaticEngineProfile;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the Magmatic Engine recipe category with JEI. */
@JeiPlugin
public class MagmaticJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_magmatic_plugin");

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/JEI");

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering magmatic recipe category");
        registration.addRecipeCategories(
            new MagmaticRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MagmaticRecipeCategory.RECIPE_TYPE, List.of(display()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(LogisticsPower.BLOCK.MAGMATIC_ENGINE, MagmaticRecipeCategory.RECIPE_TYPE);
    }

    /** Builds the single lava row from the same profile the engine runs on. */
    private static MagmaticFuelDisplay display() {
        MagmaticEngineProfile profile = MagmaticEngineProfile.of(
            LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_OUTPUT),
            LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUFFER_CAPACITY),
            Math.toIntExact(LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_TANK_CAPACITY)),
            Math.toIntExact(LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUCKET_BURN_TICKS)));
        return new MagmaticFuelDisplay(
            Fluids.LAVA,
            profile.batchMb(),
            profile.batchBurnTicks(),
            profile.coldOutputPerTick(),
            profile.warmOutputPerTick(),
            profile.hotOutputPerTick());
    }
}
