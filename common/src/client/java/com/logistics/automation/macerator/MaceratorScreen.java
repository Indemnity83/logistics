package com.logistics.automation.macerator;

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

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/macerator.png");
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

        // Progress arrow fill (25x14 sprite at UV 180,23) over the painted arrow.
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 79, topPos + 22,
                180, 23,
                arrowWidth, 14,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Energy bar fill (sprite at UV 180,42) over the painted energy indicator. Matches the Sawmill GUI.
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 56, topPos + 42 + (13 - energyHeight),
                180, 42 + (13 - energyHeight),
                7, energyHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
