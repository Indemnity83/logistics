package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.core.machine.MachineHudLines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of the processing-machine Jade integration: renders the progress synced by
 * {@link MachineServerDataProvider}. Formatting is shared in {@link MachineHudLines}.
 */
public final class MachineComponentProvider implements IBlockComponentProvider {
    public static final MachineComponentProvider INSTANCE = new MachineComponentProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("machine").toIdentifier();

    private MachineComponentProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (Component line : MachineHudLines.build(accessor.getServerData())) {
            tooltip.add(line);
        }
    }
}
