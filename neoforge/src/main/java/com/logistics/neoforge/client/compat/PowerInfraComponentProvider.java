package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.power.PowerInfraHudLines;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.cable.CableBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of the power-infrastructure Jade integration. Cables show their tier throughput; the creative
 * sink shows its drain rate and last-tick throughput, with Jade's generic (unbounded) energy bar removed.
 * Formatting is shared in {@link PowerInfraHudLines}.
 */
public final class PowerInfraComponentProvider implements IBlockComponentProvider {
    public static final PowerInfraComponentProvider INSTANCE = new PowerInfraComponentProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("power_infra").toIdentifier();

    private PowerInfraComponentProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        // Run after Jade's built-in energy element (priority 1000) so we can strip it for the sink.
        return 2000;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Block block = accessor.getBlockState().getBlock();
        if (block instanceof CableBlock cable) {
            for (Component line : PowerInfraHudLines.cable(cable.tier())) {
                tooltip.add(line);
            }
        } else if (block instanceof CreativeSinkBlock) {
            // The discard sink's buffer is effectively unbounded — remove Jade's generic energy bar.
            tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
            for (Component line : PowerInfraHudLines.creativeSink(accessor.getServerData())) {
                tooltip.add(line);
            }
        }
    }
}
