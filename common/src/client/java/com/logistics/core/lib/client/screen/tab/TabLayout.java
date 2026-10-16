package com.logistics.core.lib.client.screen.tab;

import static com.logistics.core.lib.client.screen.tab.SideTabHost.COLLAPSED;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.SCREEN_MARGIN;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.TOP_OFFSET;

/** Pure placement math for side tabs; every value is derived from the live {@link TabAnchor}. */
public final class TabLayout {

    private TabLayout() {
    }

    /** Free space between the GUI image and the screen edge on {@code side}, less the screen margin. */
    public static int margin(TabSide side, TabAnchor anchor) {
        int left = anchor.left().getAsInt();
        return switch (side) {
            case LEFT -> left - SCREEN_MARGIN;
            case RIGHT -> anchor.screenWidth().getAsInt() - (left + anchor.imageWidth().getAsInt()) - SCREEN_MARGIN;
        };
    }

    /** Whether a collapsed tab fits; when it does not, the whole side is hidden. */
    public static boolean sideFits(int margin) {
        return margin >= COLLAPSED;
    }

    /** Widest an open tab may be on a side with {@code margin}. */
    public static int openWidth(int preferredWidth, int margin) {
        return Math.min(preferredWidth, margin);
    }

    /**
     * Left edge of a tab of width {@code w}. Tabs abut the GUI image's outer edge without overlapping
     * it: right tabs grow rightward from it, left tabs leftward.
     */
    public static int tabX(TabSide side, TabAnchor anchor, int w) {
        int left = anchor.left().getAsInt();
        return switch (side) {
            case LEFT -> left - w;
            case RIGHT -> left + anchor.imageWidth().getAsInt();
        };
    }

    /** Top edge of the first tab on either side. */
    public static int stackTop(TabAnchor anchor) {
        return anchor.top().getAsInt() + TOP_OFFSET;
    }

    /** Lowest y a tab's bottom edge may reach. */
    public static int bottomLimit(TabAnchor anchor) {
        return anchor.screenHeight().getAsInt() - SCREEN_MARGIN;
    }

    public static int lerp(int from, int to, double t) {
        return from + (int) Math.round((to - from) * t);
    }
}
