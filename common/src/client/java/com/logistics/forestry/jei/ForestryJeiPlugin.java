package com.logistics.forestry.jei;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/**
 * JEI plugin that adds info pages for the farms and Humus.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 *
 * <p>Numbers come from the config defaults, not the live values: the client's config file need
 * not match the server's.
 */
@JeiPlugin
public class ForestryJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_forestry_plugin");
    private static final String INFO = "jei.logistics.info.forestry.";

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        int area = LogisticsForestry.CONFIG.FARM_AREA_SIZE.definition().defaultValue();

        info(registration, LogisticsForestry.BLOCK.CROP_FARM, "crop_farm", area);
        info(registration, LogisticsForestry.BLOCK.ARBORETUM, "arboretum", area);
        info(registration, LogisticsForestry.BLOCK.HUMUS, "humus");
    }

    /** Adds an info page for {@code item} from the lang key {@code jei.logistics.info.forestry.<key>}. */
    private static void info(IRecipeRegistration registration, ItemLike item, String key, Object... args) {
        registration.addIngredientInfo(item, Component.translatable(INFO + key, args));
    }
}
