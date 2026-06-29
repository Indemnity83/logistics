package com.logistics.automation.kiln;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Electric Kiln.
 */
public class KilnScreen extends AbstractRecipeBookScreen<KilnScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/kiln.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public KilnScreen(KilnScreenHandler handler, Inventory inventory, Component title) {
        super(handler, new KilnRecipeBookComponent(handler), inventory, title);
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

        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 10, topPos + 19 + (30 - energyHeight),
                181, 19 + (30 - energyHeight),
                12, energyHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
