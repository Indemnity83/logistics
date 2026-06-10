package com.logistics.core.machine;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Builds the machine HUD lines from the synced tag written by {@code MachineHudData}. Only a progress line
 * while the machine is actively processing — item slots and energy are already shown by Jade's built-ins.
 */
public final class MachineHudLines {

    private MachineHudLines() {}

    public static List<Component> build(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        if (NbtCompat.getBoolean(data, MachineHudData.KEY_PROCESSING, false)) {
            float progress = NbtCompat.getFloat(data, "progress", 0);
            lines.add(Component.translatable("jade.logistics.machine.progress")
                    .append(Component.literal(": "))
                    .append(Component.literal(String.format("%.0f%%", progress * 100)).withStyle(ChatFormatting.GREEN)));
        }
        return lines;
    }
}
