package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.SupplierScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Supplier GUI.
 * Displays a 3x3 grid of supply slots with item and target amount.
 */
public class SupplierScreen extends AbstractContainerScreen<SupplierScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("minecraft", "textures/gui/container/generic_54.png");

    public SupplierScreen(SupplierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }
}
