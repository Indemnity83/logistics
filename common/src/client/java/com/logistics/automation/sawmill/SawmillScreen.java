package com.logistics.automation.sawmill;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    // Shared static energy-gauge bar (gui/sprites/automation/charge.png), drawn dark for empty + bright for fill.
    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

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
        return new ScreenPosition(this.leftPos + 30, this.height / 2 - 49);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos, topPos,
                0, 0,
                imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Progress arrow fill (24x16 sprite at UV 199,35) over the painted arrow.
        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE.toIdentifier(),
                    leftPos + 79, topPos + 35,
                    199, 35,
                    arrowWidth, 16,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Energy gauge: dark "empty" bar full height, then the bright fill over the bottom `energyHeight` px.
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                CHARGE.toIdentifier(),
                12, 30,
                0, 0,
                leftPos + 10, topPos + 19,
                12, 30,
                CHARGE_EMPTY_TINT);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    CHARGE.toIdentifier(),
                    12, 30,
                    0, 30 - energyHeight,
                    leftPos + 10, topPos + 19 + (30 - energyHeight),
                    12, energyHeight);
        }
    }
}
