package com.logistics.power.engine;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Builds the engine HUD lines from the synced diagnostics tag written by {@code EngineHudData}. Pure
 * client-side formatting with no HUD-mod API, so it is shared across loaders; the per-loader Jade provider
 * adds each returned line to the tooltip.
 */
public final class EngineHudLines {

    private EngineHudLines() {}

    /**
     * Returns the engine readout lines, or an empty list when the tag carries no engine data. Stage and
     * fuel are only included when {@code showDetails} is set (the player is holding Jade's details key).
     */
    public static List<Component> build(CompoundTag data, boolean showDetails) {
        List<Component> lines = new ArrayList<>();
        String stage = NbtCompat.getString(data, EngineHudData.KEY_STAGE, "");
        if (stage.isEmpty()) {
            return lines; // not an engine, or server data not present
        }

        boolean hasHeat = NbtCompat.getBoolean(data, "hasHeat", true);

        if (hasHeat && showDetails) {
            lines.add(row("jade.logistics.engine.stage", stage, stageColor(stage)));
        }

        if (hasHeat) {
            double temp = NbtCompat.getDouble(data, "temp", 0);
            double maxTemp = NbtCompat.getDouble(data, "maxTemp", 0);
            // Current temperature always; the max only when expanded with the details key.
            String tempText = showDetails
                    ? String.format("%.0f°C (Max %.0f)", temp, maxTemp)
                    : String.format("%.0f°C", temp);
            // Colour by heat stage so the text matches the engine's visible heat tint.
            lines.add(row("jade.logistics.engine.temperature", tempText, stageColor(stage)));
        }

        lines.add(row(
                "jade.logistics.engine.output",
                String.format("%d RF/t", NbtCompat.getLong(data, "output", 0)),
                ChatFormatting.LIGHT_PURPLE));

        boolean running = NbtCompat.getBoolean(data, "running", false);
        lines.add(labelled("jade.logistics.engine.running")
                .append(Component.translatable(running ? "jade.logistics.common.yes" : "jade.logistics.common.no")
                        .withStyle(running ? ChatFormatting.GREEN : ChatFormatting.GRAY)));

        if (showDetails && NbtCompat.getBoolean(data, "stirling", false)) {
            int burnTime = NbtCompat.getInt(data, "burnTime", 0);
            int fuelTime = NbtCompat.getInt(data, "fuelTime", 0);
            if (fuelTime > 0) {
                lines.add(row(
                        "jade.logistics.engine.fuel",
                        String.format("%d / %d ticks (%.1f%%)", burnTime, fuelTime, burnTime * 100.0f / fuelTime),
                        ChatFormatting.YELLOW));
            } else {
                lines.add(labelled("jade.logistics.engine.fuel")
                        .append(Component.translatable("jade.logistics.common.none").withStyle(ChatFormatting.GRAY)));
            }
        }

        if (NbtCompat.getBoolean(data, "overheated", false)) {
            lines.add(Component.translatable("jade.logistics.engine.overheated")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        return lines;
    }

    private static Component row(String labelKey, String value, ChatFormatting valueColor) {
        return labelled(labelKey).append(Component.literal(value).withStyle(valueColor));
    }

    private static net.minecraft.network.chat.MutableComponent labelled(String labelKey) {
        return Component.translatable(labelKey).append(Component.literal(": "));
    }

    private static ChatFormatting stageColor(String stage) {
        return switch (stage) {
            case "COLD" -> ChatFormatting.BLUE;
            case "COOL" -> ChatFormatting.GREEN;
            case "WARM" -> ChatFormatting.YELLOW;
            case "HOT" -> ChatFormatting.RED;
            case "OVERHEAT" -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.WHITE;
        };
    }
}
