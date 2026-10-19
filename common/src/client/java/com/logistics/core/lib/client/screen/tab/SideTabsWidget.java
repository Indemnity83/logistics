package com.logistics.core.lib.client.screen.tab;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.compat.ClientScreenCompat;
import com.logistics.core.lib.resource.ResourceId;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

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
    private static final String ELLIPSIS = "…";
    private static final int WHITE = 0xFFFFFFFF;

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

    /**
     * The host consumes every click inside a tab, whatever the button: on 1.21.1 a click no child
     * consumes falls through to the screen's slot handling.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return host.mouseClicked(mouseX, mouseY, button == InputConstants.MOUSE_BUTTON_LEFT);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return host.isMouseOver(mouseX, mouseY);
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
            blit(side == TabSide.LEFT ? PANEL_LEFT : PANEL_RIGHT, x, y, w, h, argbTint);
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h) {
            blit(sprite, x, y, w, h, WHITE);
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h, int argbTint) {
            blit(sprite, x, y, w, h, argbTint);
        }

        /** 1.21.1's {@code blitSprite} takes no color; the tint goes through the shader color instead. */
        private void blit(ResourceId sprite, int x, int y, int w, int h, int argbTint) {
            graphics.setColor(
                    ((argbTint >> 16) & 0xFF) / 255f,
                    ((argbTint >> 8) & 0xFF) / 255f,
                    (argbTint & 0xFF) / 255f,
                    ((argbTint >>> 24) & 0xFF) / 255f);
            RenderSystem.enableBlend();
            graphics.blitSprite(sprite.toIdentifier(), x, y, w, h);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
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
            List<FormattedCharSequence> lines = font.split(text, maxWidth);
            int lineY = y;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x, lineY, argb, shadow);
                lineY += font.lineHeight;
            }
            return lines.size() * font.lineHeight;
        }

        /** Deferred to the end of the frame, so it draws above slots, labels and the carried item. */
        @Override
        public void tooltip(List<Component> lines, int mouseX, int mouseY) {
            Screen screen = ClientScreenCompat.currentScreen();
            if (screen != null) {
                screen.setTooltipForNextRenderPass(
                        lines.stream().map(Component::getVisualOrderText).toList());
            }
        }

        @Override
        public void clickSound() {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        @Override
        public TabText text() {
            return FONT_TEXT;
        }
    }
}
