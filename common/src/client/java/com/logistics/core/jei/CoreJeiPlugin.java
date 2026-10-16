package com.logistics.core.jei;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI plugin that adds an info page for the machine Upgrade Kits.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 */
@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_core_plugin");
    private static final String INFO = "jei.logistics.info.core.";

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> kits = List.of(
                new ItemStack(LogisticsCore.ITEM.INDUSTRIAL_UPGRADE_KIT),
                new ItemStack(LogisticsCore.ITEM.PRECISION_UPGRADE_KIT),
                new ItemStack(LogisticsCore.ITEM.INFERNAL_UPGRADE_KIT));
        registration.addIngredientInfo(kits, VanillaTypes.ITEM_STACK, Component.translatable(INFO + "upgrade_kit"));
    }
}
