package com.logistics.pipe;

import com.logistics.core.lib.pipe.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Builds the pipe HUD lines from the traveling items the pipe is carrying (synced for rendering, so read
 * client-side — no server data round-trip). The count shows always; the per-item breakdown shows when
 * Jade's details key is held.
 */
public final class PipeHudLines {

    /** Cap on per-item detail lines so a busy pipe can't blow up the tooltip. */
    private static final int MAX_DETAIL_ITEMS = 5;

    private PipeHudLines() {}

    public static List<Component> build(List<TravelingItem> items, boolean showDetails) {
        List<Component> lines = new ArrayList<>();
        if (items.isEmpty()) {
            return lines;
        }

        lines.add(Component.translatable("jade.logistics.pipe.items")
                .append(Component.literal(": "))
                .append(Component.literal(String.valueOf(items.size())).withStyle(ChatFormatting.WHITE)));

        if (showDetails) {
            int shown = Math.min(items.size(), MAX_DETAIL_ITEMS);
            for (int i = 0; i < shown; i++) {
                TravelingItem item = items.get(i);
                // Append the hover name as a Component so colored/renamed items keep their styling.
                Component line = Component.literal(item.getStack().getCount() + "x ")
                        .append(item.getStack().getHoverName())
                        .append(Component.literal(" → " + item.getDirection().name()))
                        .withStyle(ChatFormatting.AQUA);
                lines.add(line);
            }
            if (items.size() > shown) {
                lines.add(Component.translatable("jade.logistics.pipe.more", items.size() - shown)
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        return lines;
    }
}
