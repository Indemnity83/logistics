package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.power.engine.EngineHudLines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of the engine Jade integration: renders the diagnostics tag synced by
 * {@link EngineServerDataProvider} as tooltip lines. Formatting is shared in {@link EngineHudLines}.
 */
public final class EngineComponentProvider implements IBlockComponentProvider {
    public static final EngineComponentProvider INSTANCE = new EngineComponentProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("engine").toIdentifier();

    private EngineComponentProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        // Run after Jade's built-in energy element (priority 1000) so we can strip it below.
        return 2000;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // The engine's internal buffer is an implementation detail — remove Jade's generic energy bar.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
        for (Component line : EngineHudLines.build(accessor.getServerData(), accessor.showDetails())) {
            tooltip.add(line);
        }
    }
}
