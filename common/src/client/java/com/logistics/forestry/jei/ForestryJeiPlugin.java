package com.logistics.forestry.jei;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmGeometry;
import com.logistics.forestry.farm.FarmSettings;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/**
 * JEI plugin that adds info pages for the farms and Humus.
 * Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in fabric.mod.json.
 *
 * <p>The farm pages show the courtyard and how far each tier's ring reaches, from config.
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
        Object[] footprint = footprint();
        info(registration, LogisticsForestry.BLOCK.CROP_FARM, "crop_farm", footprint);
        info(registration, LogisticsForestry.BLOCK.ARBORETUM, "arboretum", footprint);
        info(registration, LogisticsForestry.BLOCK.HUMUS, "humus");
    }

    /** The courtyard side, then each tier's outer side, lowest tier first. */
    private static Object[] footprint() {
        FarmSettings settings = FarmSettings.fromConfig();
        List<Object> sides = new ArrayList<>();
        sides.add(FarmGeometry.COURTYARD);
        for (MachineTier tier = MachineTier.BASIC; tier != null; tier = tier.next().orElse(null)) {
            sides.add(settings.profile(tier).outerSize());
        }
        return sides.toArray();
    }

    /** Adds an info page for {@code item} from the lang key {@code jei.logistics.info.forestry.<key>}. */
    private static void info(IRecipeRegistration registration, ItemLike item, String key, Object... args) {
        registration.addIngredientInfo(item, Component.translatable(INFO + key, args));
    }
}
