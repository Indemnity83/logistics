package com.logistics.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Builds the text portion of the machine HUD from the model synced by {@code MachineHudData}: a progress
 * line while the machine is processing. Graphical entries (fluid tanks) are drawn by the loader-specific
 * Jade provider, since their element API is version-specific.
 */
public final class MachineHudLines {

    private MachineHudLines() {}

    public static List<Component> build(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        for (MachineHudModel.Entry entry : MachineHudModel.entries(data)) {
            if (entry instanceof MachineHudModel.ProgressEntry progress) {
                // Empty root so Jade's toCleanTranslation() doesn't rebuild a translatable root and drop the value.
                lines.add(Component.empty()
                        .append(Component.translatable("jade.logistics.machine.progress"))
                        .append(Component.literal(": "))
                        .append(Component.literal(String.format("%.0f%%", progress.fraction() * 100))
                                .withStyle(ChatFormatting.GREEN)));
            }
        }
        return lines;
    }
}
