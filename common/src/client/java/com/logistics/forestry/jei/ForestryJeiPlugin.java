package com.logistics.forestry.jei;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * JEI plugin that adds info pages for the farms and Humus.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 */
@JeiPlugin
public class ForestryJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_forestry_plugin");
    private static final String INFO = "jei.logistics.info.";

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Block farm : LogisticsForestry.BLOCK.farms()) {
            info(registration, farm);
        }
        info(registration, LogisticsForestry.BLOCK.HUMUS);
    }

    /** Adds an info page for {@code block} from its lang key, {@link #infoKey}. */
    private static void info(IRecipeRegistration registration, Block block) {
        registration.addIngredientInfo(block, Component.translatable(infoKey(block)));
    }

    /** {@code jei.logistics.info.forestry.crop_farm} for {@code logistics:forestry/crop_farm}. */
    private static String infoKey(Block block) {
        return INFO + BuiltInRegistries.BLOCK.getKey(block).getPath().replace('/', '.');
    }
}
