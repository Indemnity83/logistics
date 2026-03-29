package com.logistics.automation.macerator;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Iron Macerator.
 */
public class MaceratorScreen extends AbstractContainerScreen<MaceratorScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/macerator.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public MaceratorScreen(MaceratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        // Main GUI background (176x166 from UV 0,0)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos, topPos,
            0, 0,
            imageWidth, imageHeight,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Progress overlay — fills left to right at (leftPos+80, topPos+36), up to 24px wide
        // Source in texture: UV (180, 36), size 24x13
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 80, topPos + 36,
                180, 36,
                arrowWidth, 13,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Energy bar overlay — fills bottom to top at (leftPos+60, topPos+55), up to 12px tall
        // Source in texture: UV (180, 55), size 6x12
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 60, topPos + 55 + (12 - energyHeight),
                180, 55 + (12 - energyHeight),
                6, energyHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
