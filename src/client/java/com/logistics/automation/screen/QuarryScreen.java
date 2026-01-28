package com.logistics.automation.screen;

import com.logistics.automation.quarry.ui.QuarryScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class QuarryScreen extends AbstractContainerScreen<QuarryScreenHandler> {
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    public QuarryScreen(QuarryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
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
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        context.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
