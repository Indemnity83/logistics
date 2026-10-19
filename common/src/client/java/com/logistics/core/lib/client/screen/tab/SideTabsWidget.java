package com.logistics.core.lib.client.screen.tab;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * The screen-side view of a {@link SideTabHost}: draws it and forwards input to it. The only class of
 * the tab framework that touches Minecraft's render and input APIs. Create it through
 * {@code host.attach(SideTabsWidget::new)} and add it with {@code addRenderableWidget}.
 *
 * <p>It never takes focus and has no narration of its own.
 */
public final class SideTabsWidget implements Renderable, GuiEventListener, NarratableEntry {
    private static final ResourceId PANEL_LEFT = LogisticsMod.modId("tab/panel_left");
    private static final ResourceId PANEL_RIGHT = LogisticsMod.modId("tab/panel_right");
    private static final TabText FONT_TEXT = new FontText();
    private static final String ELLIPSIS = "\u2026";

    private final SideTabHost host;

    public SideTabsWidget(SideTabHost host) {
        this.host = host;
    }

    /** Text measurement backed by the client font; the same metrics the widget draws with. */
    public static TabText fontText() {
        return FONT_TEXT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        host.render(new Canvas(graphics), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return host.mouseClicked(event.x(), event.y(), event.button() == InputConstants.MOUSE_BUTTON_LEFT);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return host.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static final class FontText implements TabText {
        @Override
        public int width(Component text) {
            return font().width(text);
        }

        @Override
        public int lineHeight() {
            return font().lineHeight;
        }

        @Override
        public int wrappedLineCount(Component text, int maxWidth) {
            return font().split(text, maxWidth).size();
        }

        @Override
        public Component ellipsize(Component text, int maxWidth) {
            Font font = font();
            if (font.width(text) <= maxWidth) {
                return text;
            }
            String prefix = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width(ELLIPSIS)));
            return Component.literal(prefix + ELLIPSIS);
        }
    }

    private static final class Canvas implements TabCanvas {
        private final GuiGraphics graphics;

        Canvas(GuiGraphics graphics) {
            this.graphics = graphics;
        }

        @Override
        public void panel(TabSide side, int x, int y, int w, int h, int argbTint) {
            ResourceId sprite = side == TabSide.LEFT ? PANEL_LEFT : PANEL_RIGHT;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.toIdentifier(), x, y, w, h, argbTint);
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.toIdentifier(), x, y, w, h);
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h, int argbTint) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.toIdentifier(), x, y, w, h, argbTint);
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            graphics.fill(x0, y0, x1, y1, argb);
        }

        @Override
        public void text(Component text, int x, int y, int argb, boolean shadow) {
            graphics.drawString(font(), text, x, y, argb, shadow);
        }

        @Override
        public int wrappedText(Component text, int x, int y, int maxWidth, int argb, boolean shadow) {
            Font font = font();
            graphics.drawWordWrap(font, text, x, y, maxWidth, argb, shadow);
            return font.split(text, maxWidth).size() * font.lineHeight;
        }

        @Override
        public void tooltip(List<Component> lines, int mouseX, int mouseY) {
            graphics.setTooltipForNextFrame(font(), lines, Optional.empty(), mouseX, mouseY);
        }

        @Override
        public void clickSound() {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }

        @Override
        public TabText text() {
            return FONT_TEXT;
        }
    }
}
