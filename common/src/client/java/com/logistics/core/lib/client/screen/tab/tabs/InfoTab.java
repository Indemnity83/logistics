package com.logistics.core.lib.client.screen.tab.tabs;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.screen.tab.SideTab;
import com.logistics.core.lib.client.screen.tab.TabCanvas;
import com.logistics.core.lib.client.screen.tab.TabSide;
import com.logistics.core.lib.client.screen.tab.TabText;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.network.chat.Component;

/** Left tab with a short description of the machine, {@code gui.logistics.tab.info.<key>}. */
public final class InfoTab extends SideTab {
    public static final String ID = "info";
    public static final String TITLE_KEY = "gui.logistics.tab.info.title";
    public static final String TEXT_KEY_PREFIX = "gui.logistics.tab.info.";
    public static final int TINT = 0xFF8A8A8A;
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int PREFERRED_WIDTH = 120;
    public static final int MIN_OPEN_WIDTH = 70;

    private static final ResourceId ICON = LogisticsMod.modId("tab/info");

    private final Component text;

    /** @param key the machine's key; the body text is {@code gui.logistics.tab.info.<key>} */
    public InfoTab(String key) {
        this.text = Component.translatable(textKey(key));
    }

    public static String textKey(String key) {
        return TEXT_KEY_PREFIX + key;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public TabSide side() {
        return TabSide.LEFT;
    }

    @Override
    public int tint() {
        return TINT;
    }

    @Override
    public ResourceId icon() {
        return ICON;
    }

    @Override
    public Component title() {
        return Component.translatable(TITLE_KEY);
    }

    @Override
    public int preferredWidth() {
        return PREFERRED_WIDTH;
    }

    @Override
    public int minOpenWidth() {
        return MIN_OPEN_WIDTH;
    }

    @Override
    public int contentHeight(TabText text, int contentWidth) {
        return text.wrappedLineCount(this.text, contentWidth) * text.lineHeight();
    }

    @Override
    public void drawContent(TabCanvas canvas, int x, int y, int contentWidth, int mouseX, int mouseY) {
        canvas.wrappedText(text, x, y, contentWidth, TEXT_COLOR);
    }
}
