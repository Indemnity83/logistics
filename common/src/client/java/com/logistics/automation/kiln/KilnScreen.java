package com.logistics.automation.kiln;

import com.logistics.LogisticsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Electric Kiln.
 */
public class KilnScreen extends AbstractContainerScreen<KilnScreenHandler> {

    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/kiln.png").toIdentifier();

    public KilnScreen(KilnScreenHandler handler, Inventory inventory, Component title) {
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
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, leftPos + 80, topPos + 36, 180, 36, arrowWidth, 14);
        }

        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blit(TEXTURE, leftPos + 60, topPos + 55 + (13 - energyHeight),
                180, 55 + (13 - energyHeight), 7, energyHeight);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
