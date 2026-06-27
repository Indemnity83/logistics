package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns ResourceLocation
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration for fluid pipes. A fluid extractor pipe holds a small power buffer to drive its arm, but
 * that buffer is an internal implementation detail (as with item pipes), so this strips Jade's generic energy
 * bar. Fluid contents are left to Jade's built-in fluid element.
 *
 * <p>Scoped to fluid pipes so the item-pipe {@link PipeComponentProvider} (which reads a {@code PipeBlockEntity})
 * is never registered against a {@code FluidPipeBlockEntity}.
 */
public final class FluidPipeComponentProvider implements IBlockComponentProvider {
    public static final FluidPipeComponentProvider INSTANCE = new FluidPipeComponentProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by ResourceLocation
            LogisticsMod.modId("fluid_pipe").toIdentifier();

    private FluidPipeComponentProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning ResourceLocation
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        // Run after Jade's built-in energy element (priority 1000) so we can strip it below.
        return 2000;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // A fluid pipe's energy buffer is an internal implementation detail — drop Jade's generic energy bar.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
    }
}
