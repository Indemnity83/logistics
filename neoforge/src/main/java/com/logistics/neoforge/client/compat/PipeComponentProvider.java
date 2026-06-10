package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeHudLines;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration for pipes: shows how many items are in transit and, on the details key, each item's
 * destination and progress. Traveling items are synced for rendering, so this reads the client block
 * entity directly — no server-data provider needed. Formatting is shared in {@link PipeHudLines}.
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
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof PipeBlockEntity pipe) {
            for (Component line : PipeHudLines.build(pipe.getTravelingItems(), accessor.showDetails())) {
                tooltip.add(line);
            }
        }
    }
}
