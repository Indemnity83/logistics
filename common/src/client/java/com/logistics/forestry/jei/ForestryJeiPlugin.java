package com.logistics.forestry.jei;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.forestry.farm.FarmMachineBlock;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * JEI plugin that adds info pages for the farms and Humus.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 *
 * <p>The plot size shown is the outer side of each farm's ring, fixed per farm type.
 */
@JeiPlugin
public class ForestryJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_forestry_plugin");
    private static final String INFO = "jei.logistics.info.forestry.";

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, LogisticsForestry.BLOCK.CROP_FARM, "crop_farm",
                ringOuter(LogisticsForestry.BLOCK.CROP_FARM),
                ringInner(LogisticsForestry.BLOCK.CROP_FARM));
        info(registration, LogisticsForestry.BLOCK.ARBORETUM, "arboretum",
                ringOuter(LogisticsForestry.BLOCK.ARBORETUM),
                ringInner(LogisticsForestry.BLOCK.ARBORETUM));
        info(registration, LogisticsForestry.BLOCK.HUMUS, "humus");
    }

    private static int ringOuter(Block farm) {
        return ((FarmMachineBlock) farm).logic().ringOuter();
    }

    private static int ringInner(Block farm) {
        return ((FarmMachineBlock) farm).logic().ringInner();
    }

    /** Adds an info page for {@code item} from the lang key {@code jei.logistics.info.forestry.<key>}. */
    private static void info(IRecipeRegistration registration, ItemLike item, String key, Object... args) {
        registration.addIngredientInfo(item, Component.translatable(INFO + key, args));
    }
}
