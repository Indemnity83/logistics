package com.logistics.core.lib.client.screen.tab;

import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * One side tab. The host owns placement and animation; a tab supplies its identity, its look and
 * its open-state content. Content coordinates are relative to the content origin, which sits
 * {@link SideTabHost#CONTENT_PAD} in from the tab's left edge and {@link SideTabHost#HEADER} below
 * its top.
 */
public abstract class SideTab {

    /** Stable id, unique within a host; also the key remembered in {@link TabMemory}. */
    public abstract String id();

    public abstract TabSide side();

    /** ARGB color multiplied onto the grayscale panel. */
    public abstract int tint();

    /** 16×16 GUI sprite shown on the collapsed tab and in the open header. */
    public abstract ResourceId icon();

    public abstract Component title();

    /** Extra lines under the title in the collapsed tooltip. */
    public List<Component> statusLines() {
        return List.of();
    }

    /** Desired open width, before it is clamped to the screen margin. */
    public abstract int preferredWidth();

    /** Narrowest open width the content still works at; below it the tab cannot open. */
    public abstract int minOpenWidth();

    /** Content height at {@code contentWidth}, excluding the header and bottom padding. */
    public abstract int contentHeight(TabText text, int contentWidth);

    /** Draws the content; called only while the tab is fully open. */
    public abstract void drawContent(TabCanvas canvas, int x, int y, int contentWidth, int mouseX, int mouseY);

    /**
     * Handles a primary click below the header of a fully open tab. Coordinates are local to the
     * content origin and may be negative inside the side padding.
     *
     * @return true when the click did something (plays the click sound)
     */
    public boolean clickContent(double localX, double localY) {
        return false;
    }

    /** Tooltip for a point in the content, local to the content origin; empty for none. */
    public List<Component> contentTooltip(double localX, double localY) {
        return List.of();
    }
}
