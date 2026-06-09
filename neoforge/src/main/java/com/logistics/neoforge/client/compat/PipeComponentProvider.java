package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeHudLines;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * Jade integration for pipes: items in transit, plus each module's status (filters, chassis module configs)
 * and a row of installed-module icons for chassis pipes. Reads the client block entity directly — module
 * and traveling-item state are both synced. Formatting is shared in {@link PipeHudLines}.
 */
public final class PipeComponentProvider implements IBlockComponentProvider {
    public static final PipeComponentProvider INSTANCE = new PipeComponentProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("pipe").toIdentifier();

    private PipeComponentProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        // Run after Jade's built-in energy element (priority 1000) so we can strip it below.
        return 2000;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof PipeBlockEntity pipe)) {
            return;
        }
        // A pipe's energy buffer is an internal implementation detail — drop Jade's generic energy bar.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);

        boolean details = accessor.showDetails();
        for (Component line : PipeHudLines.build(pipe.getTravelingItems(), details)) {
            tooltip.add(line);
        }

        // One row per installed chassis module: a half-size item icon, a little padding, then its name.
        for (ItemStack module : PipeHudLines.installedModules(accessor.getBlockState(), pipe)) {
            tooltip.add(JadeUI.item(module, 0.66f));
            tooltip.append(JadeUI.spacer(3, 0));
            tooltip.append(module.getHoverName());
        }

        for (Component line : PipeHudLines.moduleLines(accessor.getBlockState(), pipe, details)) {
            tooltip.add(line);
        }
    }
}
