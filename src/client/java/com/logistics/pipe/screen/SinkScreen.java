package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.SinkScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Sink GUI (Basic Logistics Pipe).
 * Displays 9 filter slots and a Default Route toggle button.
 */
public class SinkScreen extends AbstractContainerScreen<SinkScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/sink.png");

    private Button defaultRouteButton;

    public SinkScreen(SinkScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 48;
    }

    @Override
    protected void init() {
        super.init();

        // Create Default Route button
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = leftPos + 68;  // Aligned to right (was 8 + 160 = 168, now 138 + 30 = 168)
        int buttonY = topPos + 37;  // Vertically centered between 35 and 58 (accounting for button height)

        this.defaultRouteButton = Button.builder(
                getDefaultRouteButtonText(),
                button -> {
                    // Toggle default route
                    boolean newValue = !menu.isDefaultRoute();
                    menu.setDefaultRoute(newValue);
                    button.setMessage(getDefaultRouteButtonText());
                })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        addRenderableWidget(defaultRouteButton);
    }

    private Component getDefaultRouteButtonText() {
        Component prefix = Component.translatable("gui.logistics.default_route");
        Component value = Component.translatable(
                menu.isDefaultRoute() ? "gui.logistics.yes" : "gui.logistics.no");
        return Component.empty().append(prefix).append(" ").append(value);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        // Draw background texture
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Update button text in case it changed
        defaultRouteButton.setMessage(getDefaultRouteButtonText());

        renderTooltip(graphics, mouseX, mouseY);
    }
}
