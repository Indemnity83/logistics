package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.ProcessScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Process Logistics Pipe GUI.
 *
 * <p>Layout: 9 input ghost slots in a row at (8,18), single output ghost slot (24px box) at (84,50),
 * satellite selector centered at (142,50), player inventory at (8,92), hotbar at (8,150).
 */
public class ProcessScreen extends AbstractContainerScreen<ProcessScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/process.png");

    // Satellite selector centered at (142, 50) within the background
    private static final int SAT_CENTER_X = 142;
    private static final int SAT_CENTER_Y = 53;
    private static final int BTN_WIDTH = 11;
    private static final int BTN_HEIGHT = 11;
    private static final int SAT_BTN_Y = SAT_CENTER_Y - BTN_HEIGHT / 2; // 43

    public ProcessScreen(ProcessScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
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
                .bounds(leftPos + 118, topPos + SAT_BTN_Y, BTN_WIDTH, BTN_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                })
                .bounds(leftPos + 155, topPos + SAT_BTN_Y, BTN_WIDTH, BTN_HEIGHT)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int satId = menu.getCurrentSatelliteId();
        String label = satId > 0 ? String.valueOf(satId) : "Off";
        int color = (satId <= 0 || menu.isSatelliteRegistered()) ? 0xFFFFFFFF : 0xFF484848;
        graphics.drawCenteredString(font, label, SAT_CENTER_X, SAT_CENTER_Y - font.lineHeight / 2, color);

        Component satelliteLabel = Component.literal("Satellite");
        FormattedCharSequence formattedCharSequence = satelliteLabel.getVisualOrderText();
        graphics.drawString(font, satelliteLabel, SAT_CENTER_X - font.width(formattedCharSequence) / 2, 6, 0xFF484848, false);

        graphics.drawString(font, Component.literal("Output"), 45, 50, 0xFF484848, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
