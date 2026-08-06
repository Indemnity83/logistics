package com.logistics.pipe.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * A compact button with smaller-than-default text, for controls where vanilla {@code Button}'s 8px
 * font reads as oversized (e.g. the fluid supplier's step-amount buttons). Draws a simple flat
 * background — no external texture dependency — and centers its label at {@link #TEXT_SCALE} of the
 * default font size via a pushed/scaled pose transform.
 */
public class SmallStepButton extends AbstractWidget {
    private static final float TEXT_SCALE = 0.6f;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int FILL_COLOR = 0xFFC6C6C6;
    private static final int FILL_COLOR_HOVERED = 0xFFDCDCDC;
    private static final int TEXT_COLOR = 0xFF404040;

    private final Font font;
    private final Runnable onPress;

    public SmallStepButton(int x, int y, int width, int height, Font font, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.font = font;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = (isHoveredOrFocused() && active) ? FILL_COLOR_HOVERED : FILL_COLOR;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), BORDER_COLOR);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, fill);

        Component message = getMessage();
        float textWidth = font.width(message) * TEXT_SCALE;
        float textHeight = font.lineHeight * TEXT_SCALE;
        float textX = getX() + (getWidth() - textWidth) / 2f;
        float textY = getY() + (getHeight() - textHeight) / 2f;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(textX, textY);
        pose.scale(TEXT_SCALE, TEXT_SCALE);
        graphics.text(font, message, 0, 0, TEXT_COLOR, false);
        pose.popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (active && !doubleClick) {
            onPress.run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
