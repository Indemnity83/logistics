package com.logistics.core.lib.client.screen.tab;

import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Drawing surface for side tabs; implemented per Minecraft version by the widget layer. */
public interface TabCanvas {
    /** The tab background, a grayscale nine-slice multiplied by {@code argbTint}. */
    void panel(TabSide side, int x, int y, int w, int h, int argbTint);

    void sprite(ResourceId sprite, int x, int y, int w, int h);

    void sprite(ResourceId sprite, int x, int y, int w, int h, int argbTint);

    /** Fills {@code [x0, x1) × [y0, y1)}. */
    void fill(int x0, int y0, int x1, int y1, int argb);

    void text(Component text, int x, int y, int argb, boolean shadow);

    /** Draws {@code text} word-wrapped to {@code maxWidth} and returns the height it used. */
    int wrappedText(Component text, int x, int y, int maxWidth, int argb);

    /** Queues a tooltip for this frame. */
    void tooltip(List<Component> lines, int mouseX, int mouseY);

    void clickSound();

    TabText text();
}
