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

    // Shared static energy-gauge bar sprite, drawn dark for empty + bright for fill.
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();

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
            graphics.blit(TEXTURE, leftPos + 79, topPos + 35, 199, 35, arrowWidth, 16);
        }

        // Energy gauge: dark "empty" bar full height, then the bright fill over the bottom `energyHeight` px.
        graphics.setColor(0.25f, 0.25f, 0.25f, 1.0f);
        graphics.blitSprite(CHARGE, 12, 30, 0, 0, leftPos + 10, topPos + 19, 12, 30);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(CHARGE, 12, 30, 0, 30 - energyHeight,
                leftPos + 10, topPos + 19 + (30 - energyHeight), 12, energyHeight);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
