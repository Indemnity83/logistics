package com.logistics.power;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.power.cable.CableTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Builds the power-infrastructure HUD lines. Cable throughput comes straight from the tier (client-side);
 * the creative sink's lines come from the synced tag written by {@code PowerInfraHudData}. Pure formatting,
 * shared across loaders; the per-loader Jade provider adds the returned lines to the tooltip.
 */
public final class PowerInfraHudLines {

    private PowerInfraHudLines() {}

    /** Cable: tier throughput. */
    public static List<Component> cable(CableTier tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(row(
                "jade.logistics.cable.transfer",
                String.format("%d RF/t", tier.transferRate()),
                ChatFormatting.AQUA));
        return lines;
    }

    /** Creative sink: configured drain rate and energy discarded last tick. */
    public static List<Component> creativeSink(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        if (!PowerInfraHudData.TYPE_CREATIVE_SINK.equals(NbtCompat.getString(data, PowerInfraHudData.KEY_TYPE, ""))) {
            return lines; // server data not present
        }
        lines.add(row(
                "jade.logistics.creative_sink.drain",
                String.format("%d RF/t", NbtCompat.getLong(data, PowerInfraHudData.KEY_DRAIN_RATE, 0)),
                ChatFormatting.AQUA));
        lines.add(row(
                "jade.logistics.creative_sink.received",
                String.format("%d RF/t", NbtCompat.getLong(data, PowerInfraHudData.KEY_RECEIVED, 0)),
                ChatFormatting.GREEN));
        return lines;
    }

    private static Component row(String labelKey, String value, ChatFormatting valueColor) {
        // Empty root so Jade's toCleanTranslation() doesn't rebuild a translatable root and drop the value.
        return Component.empty()
                .append(Component.translatable(labelKey))
                .append(Component.literal(": "))
                .append(Component.literal(value).withStyle(valueColor));
    }
}
