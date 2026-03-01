package com.logistics.pipe.screen.widget;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
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

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (active && !bl) {
            onPress.run();
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
