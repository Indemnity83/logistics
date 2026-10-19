package com.logistics.core.lib.client.screen.tab;

import net.minecraft.network.chat.Component;

/** Text measurement used for tab layout, so layout never touches the font directly. */
public interface TabText {
    int width(Component text);

    int lineHeight();

    /** Number of lines {@code text} occupies when word-wrapped to {@code maxWidth}. */
    int wrappedLineCount(Component text, int maxWidth);

    /** {@code text} unchanged when it fits {@code maxWidth}, else its longest prefix that fits with "…" appended. */
    Component ellipsize(Component text, int maxWidth);
}
