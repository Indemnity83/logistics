package com.logistics.automation.laserquarry;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Builds the laser-quarry HUD lines from the synced status tag written by {@code QuarryHudData}. Phase and
 * any no-power warning show always; power-in and arm speed are detail lines gated behind Jade's details key.
 */
public final class QuarryHudLines {

    private QuarryHudLines() {}

    public static List<Component> build(CompoundTag data, boolean showDetails) {
        List<Component> lines = new ArrayList<>();
        String phase = NbtCompat.getString(data, QuarryHudData.KEY_PHASE, "");
        if (phase.isEmpty()) {
            return lines; // not a quarry, or server data not present
        }
        boolean finished = NbtCompat.getBoolean(data, "finished", false);

        String phaseKey = finished ? "jade.logistics.quarry.phase.finished" : phaseKey(phase);
        lines.add(Component.empty()
                .append(Component.translatable("jade.logistics.quarry.phase"))
                .append(Component.literal(": "))
                .append(Component.translatable(phaseKey).withStyle(ChatFormatting.AQUA)));

        if (!finished && showDetails) {
            lines.add(row(
                    "jade.logistics.quarry.power_in",
                    String.format("%,d RF/t", NbtCompat.getLong(data, "powerIn", 0)),
                    ChatFormatting.GREEN));
            if ("MINING".equals(phase)) {
                lines.add(row(
                        "jade.logistics.quarry.arm_speed",
                        String.format("%.2f blocks/tick", NbtCompat.getFloat(data, "armSpeed", 0)),
                        ChatFormatting.LIGHT_PURPLE));
            }
        }

        return lines;
    }

    private static String phaseKey(String phase) {
        return switch (phase) {
            case "CLEARING" -> "jade.logistics.quarry.phase.clearing";
            case "BUILDING_FRAME" -> "jade.logistics.quarry.phase.building_frame";
            case "MAINTAINING_CLEARANCE" -> "jade.logistics.quarry.phase.maintaining_clearance";
            case "REPAIRING_FRAME" -> "jade.logistics.quarry.phase.repairing_frame";
            default -> "jade.logistics.quarry.phase.mining";
        };
    }

    private static Component row(String labelKey, String value, ChatFormatting valueColor) {
        // Empty root so Jade's toCleanTranslation() doesn't rebuild a translatable root and drop the value.
        return Component.empty()
                .append(Component.translatable(labelKey))
                .append(Component.literal(": "))
                .append(Component.literal(value).withStyle(valueColor));
    }
}
