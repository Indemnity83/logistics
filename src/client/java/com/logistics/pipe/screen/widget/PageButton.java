package com.logistics.pipe.screen.widget;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Pagination button that renders page_forward.png or page_backward.png texture.
 * Button is 12×17 pixels.
 */
public class PageButton extends AbstractWidget {
    private final ResourceId normalTexture;
    private final ResourceId highlightedTexture;
    private final Runnable onPress;

    public PageButton(int x, int y, boolean forward, Runnable onPress) {
        super(x, y, 12, 17, Component.empty());
        this.onPress = onPress;

        if (forward) {
            this.normalTexture = LogisticsMod.modId("textures/gui/pipe/page_forward.png");
            this.highlightedTexture = LogisticsMod.modId("textures/gui/pipe/page_forward_highlighted.png");
        } else {
            this.normalTexture = LogisticsMod.modId("textures/gui/pipe/page_backward.png");
            this.highlightedTexture = LogisticsMod.modId("textures/gui/pipe/page_backward_highlighted.png");
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Use highlighted texture when hovered and active
        ResourceId texture = (isHoveredOrFocused() && active) ? highlightedTexture : normalTexture;

        // MC 1.21.1: blit() without RenderPipeline param
        graphics.blit(
                texture.toIdentifier(),
                getX(),
                getY(),
                0,
                0,
                12,
                17,
                12,
                17
        );
    }

    // MC 1.21.1: mouseClicked uses (double, double, int) instead of MouseButtonEvent
    // Must check isMouseOver — MC 1.21.1 passes clicks to all children without pre-filtering by bounds
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && button == 0 && isMouseOver(mouseX, mouseY)) {
            onPress.run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
