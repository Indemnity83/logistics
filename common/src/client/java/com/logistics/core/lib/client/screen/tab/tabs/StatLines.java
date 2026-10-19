package com.logistics.core.lib.client.screen.tab.tabs;

import com.logistics.core.lib.client.screen.tab.TabCanvas;
import com.logistics.core.lib.client.screen.tab.TabText;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * Label/value groups as the status tabs show them: the label, then its value on the line below,
 * indented, with a little space between groups. Both wrap to the content width.
 */
final class StatLines {
    static final int LABEL_COLOR = 0xFFDDE4F0;
    static final int VALUE_COLOR = 0xFFFFFFFF;
    /** Indent of a value under its label. */
    static final int VALUE_INDENT = 4;
    /** Extra space between consecutive groups. */
    static final int GROUP_SPACING = 1;
    /** Shown in place of a value that is not known yet. */
    static final Component UNKNOWN = Component.literal("—");

    private StatLines() {
    }

    record Group(Component label, Component value, int valueColor) {
        Group(Component label, Component value) {
            this(label, value, VALUE_COLOR);
        }
    }

    /** {@code n} with grouping separators, the same in every locale. */
    static String number(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    static int height(TabText text, int width, List<Group> groups) {
        int lines = 0;
        for (Group group : groups) {
            lines += text.wrappedLineCount(group.label(), width);
            lines += text.wrappedLineCount(group.value(), valueWidth(width));
        }
        return lines * text.lineHeight() + Math.max(0, groups.size() - 1) * GROUP_SPACING;
    }

    static void draw(TabCanvas canvas, int x, int y, int width, List<Group> groups) {
        for (Group group : groups) {
            y += canvas.wrappedText(group.label(), x, y, width, LABEL_COLOR, true);
            y += canvas.wrappedText(group.value(), x + VALUE_INDENT, y, valueWidth(width), group.valueColor(), true);
            y += GROUP_SPACING;
        }
    }

    private static int valueWidth(int width) {
        return Math.max(1, width - VALUE_INDENT);
    }
}
