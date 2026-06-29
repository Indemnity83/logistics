package com.logistics.automation.sawmill;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Sawmill GUI.
 * Shows the input, primary/secondary outputs, a saw-progress arrow, and an energy bar.
 */
public class SawmillScreen extends AbstractRecipeBookScreen<SawmillScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/sawmill.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public SawmillScreen(SawmillScreenHandler handler, Inventory inventory, Component title) {
        super(handler, new SawmillRecipeBookComponent(handler), inventory, title);
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
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos, topPos,
                0, 0,
                imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Progress arrow fill (sprite at UV 180,36) over the painted arrow.
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE.toIdentifier(),
                    leftPos + 77, topPos + 24,
                    183, 24,
                    arrowWidth, 14,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Energy bar fill (sprite at UV 180,55) over the painted energy indicator.
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
