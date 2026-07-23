package com.logistics.power.engine;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

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

        boolean hasHeat = NbtCompat.getBoolean(data, EngineHudData.KEY_HAS_HEAT, true);

        if (hasHeat && showDetails) {
            lines.add(row("jade.logistics.engine.stage", stage, stageColor(stage)));
        }

        if (hasHeat) {
            double temp = NbtCompat.getDouble(data, EngineHudData.KEY_TEMP, 0);
            double maxTemp = NbtCompat.getDouble(data, EngineHudData.KEY_MAX_TEMP, 0);
            // Current temperature always; the max only when expanded with the details key.
            String tempText = showDetails
                    ? String.format("%.0f°C (Max %.0f)", temp, maxTemp)
                    : String.format("%.0f°C", temp);
            // Colour by heat stage so the text matches the engine's visible heat tint.
            lines.add(row("jade.logistics.engine.temperature", tempText, stageColor(stage)));
        }

        lines.add(row(
                "jade.logistics.engine.output",
                String.format("%d RF/t", NbtCompat.getLong(data, EngineHudData.KEY_OUTPUT, 0)),
                ChatFormatting.LIGHT_PURPLE));

        boolean running = NbtCompat.getBoolean(data, EngineHudData.KEY_RUNNING, false);
        lines.add(labelled("jade.logistics.engine.running")
                .append(Component.translatable(running ? "jade.logistics.common.yes" : "jade.logistics.common.no")
                        .withStyle(running ? ChatFormatting.GREEN : ChatFormatting.GRAY)));

        if (showDetails && NbtCompat.getBoolean(data, EngineHudData.KEY_STIRLING, false)) {
            int burnTime = NbtCompat.getInt(data, EngineHudData.KEY_BURN_TIME, 0);
            int fuelTime = NbtCompat.getInt(data, EngineHudData.KEY_FUEL_TIME, 0);
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

        if (NbtCompat.getBoolean(data, EngineHudData.KEY_MAGMATIC_ENGINE, false)) {
            lines.add(row(
                    "jade.logistics.engine.generation",
                    String.format("%d RF/t", NbtCompat.getLong(data, EngineHudData.KEY_MAGMATIC_ATTEMPTED, 0)),
                    ChatFormatting.LIGHT_PURPLE));
            lines.add(row("jade.logistics.engine.temperature",
                    String.format("%d°C", NbtCompat.getInt(data, EngineHudData.KEY_MAGMATIC_TEMP, 0)),
                    ChatFormatting.RED));
            if (showDetails) {
                lines.add(fluidRow("jade.logistics.engine.lava",
                        NbtCompat.getString(data, EngineHudData.KEY_LAVA_FLUID, ""),
                        NbtCompat.getInt(data, EngineHudData.KEY_LAVA_AMOUNT, 0)));
            }
        }

        if (NbtCompat.getBoolean(data, EngineHudData.KEY_OVERHEATED, false)) {
            lines.add(Component.translatable("jade.logistics.engine.overheated")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        return lines;
    }

    private static Component fluidRow(String labelKey, String fluidId, int amountMb) {
        if (fluidId.isEmpty() || amountMb <= 0) {
            return labelled(labelKey)
                    .append(Component.translatable("jade.logistics.common.none").withStyle(ChatFormatting.GRAY));
        }
        ResourceId id = ResourceId.tryParse(fluidId);
        Fluid fluid = id == null ? null : BuiltInRegistries.FLUID.getValue(id.toIdentifier());
        Component name = fluid == null ? Component.literal(fluidId) : FluidDisplay.name(fluid);
        return labelled(labelKey)
                .append(name.copy().withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.format(" (%d mB)", amountMb)).withStyle(ChatFormatting.GRAY));
    }

    private static Component row(String labelKey, String value, ChatFormatting valueColor) {
        return labelled(labelKey).append(Component.literal(value).withStyle(valueColor));
    }

    private static net.minecraft.network.chat.MutableComponent labelled(String labelKey) {
        // Root must NOT be a bare no-args translatable: Jade's TextElement runs each line through
        // JadeLanguages.toCleanTranslation(), which rebuilds such a component from its key alone and
        // drops appended siblings (the ": " + value). An empty root sidesteps that and keeps the value.
        return Component.empty().append(Component.translatable(labelKey)).append(Component.literal(": "));
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
