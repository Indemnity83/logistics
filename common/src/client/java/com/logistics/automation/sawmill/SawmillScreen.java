package com.logistics.automation.sawmill;

import com.logistics.LogisticsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Sawmill GUI.
 * Shows the input, primary/secondary outputs, a saw-progress arrow, and an energy bar.
 */
public class SawmillScreen extends AbstractContainerScreen<SawmillScreenHandler> {

    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/sawmill.png").toIdentifier();

    public SawmillScreen(SawmillScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Progress arrow fill (sprite at UV 183,24) over the painted arrow.
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, leftPos + 77, topPos + 24, 183, 24, arrowWidth, 14);
        }

        // Energy bar fill (sprite at UV 180,42) over the painted energy indicator.
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(
                TEXTURE, leftPos + 56, topPos + 42 + (13 - energyHeight), 180, 42 + (13 - energyHeight), 7, energyHeight);
        }
    }
}
