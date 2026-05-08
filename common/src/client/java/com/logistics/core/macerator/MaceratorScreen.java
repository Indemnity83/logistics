package com.logistics.core.macerator;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Iron Macerator.
 */
public class MaceratorScreen extends AbstractRecipeBookScreen<MaceratorScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/core/macerator.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public MaceratorScreen(MaceratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, new MaceratorRecipeBookComponent(handler), inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 20, this.height / 2 - 49);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Main GUI background (176x166 from UV 0,0)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos, topPos,
            0, 0,
            imageWidth, imageHeight,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Progress overlay — fills left to right at (leftPos+80, topPos+36), up to 25px wide
        // Source in texture: UV (180, 36), size 25x14
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 80, topPos + 36,
                180, 36,
                arrowWidth, 14,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Energy bar overlay — fills bottom to top at (leftPos+60, topPos+55), up to 13px tall
        // Source in texture: UV (180, 55), size 7x13
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 60, topPos + 55 + (13 - energyHeight),
                180, 55 + (13 - energyHeight),
                7, energyHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
