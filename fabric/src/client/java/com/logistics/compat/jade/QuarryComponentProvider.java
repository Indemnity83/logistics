package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.QuarryHudLines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of the laser-quarry Jade integration: renders the status tag synced by
 * {@link QuarryServerDataProvider}. Formatting is shared in {@link QuarryHudLines}.
 */
public final class QuarryComponentProvider implements IBlockComponentProvider {
    public static final QuarryComponentProvider INSTANCE = new QuarryComponentProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("quarry").toIdentifier();

    private QuarryComponentProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (Component line : QuarryHudLines.build(accessor.getServerData(), accessor.showDetails())) {
            tooltip.add(line);
        }
    }
}
