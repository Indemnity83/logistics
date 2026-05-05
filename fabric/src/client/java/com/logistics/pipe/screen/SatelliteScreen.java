package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.SatelliteScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Satellite Logistics Pipe GUI.
 * Shows a numeric satellite ID with +/- buttons; player inventory shown below.
 * The ID number is centered at (87, 20) within the background.
 */
public class SatelliteScreen extends AbstractContainerScreen<SatelliteScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/satellite.png");

    // Layout: number centered at x=87, y=20 within the background image
    private static final int CENTER_X = 87;
    private static final int CENTER_Y = 20;
    private static final int BTN_WIDTH = 20;
    private static final int BTN_HEIGHT = 14;
    private static final int BTN_Y = CENTER_Y - BTN_HEIGHT / 2; // 13
    private static final int MINUS_X = CENTER_X - 34; // 53
    private static final int PLUS_X = CENTER_X + 14;  // 101

    public SatelliteScreen(SatelliteScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 122;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                })
                .bounds(leftPos + MINUS_X, topPos + BTN_Y, BTN_WIDTH, BTN_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                })
                .bounds(leftPos + PLUS_X, topPos + BTN_Y, BTN_WIDTH, BTN_HEIGHT)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw satellite ID number centered in the top strip
        int currentId = menu.getCurrentId();
        String label = currentId > 0 ? String.valueOf(currentId) : "-";
        int color = menu.isCurrentIdTaken() ? 0xFFFF5555 : 0xFFFFFFFF;
        graphics.drawCenteredString(font, label, CENTER_X, CENTER_Y - font.lineHeight / 2, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
