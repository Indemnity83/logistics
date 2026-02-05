package com.logistics.pipe.screen;

import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterScreenHandler> {
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_INNER_SIZE = 16;

    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);

        int swatchX = leftPos + SLOT_START_X;
        int swatchY = topPos + SLOT_START_Y;
        for (int row = 0; row < ItemFilterModule.FILTER_ORDER.length; row++) {
            net.minecraft.core.Direction direction = ItemFilterModule.FILTER_ORDER[row];
            int color = ItemFilterModule.getFilterColor(direction);
            int yOffset = swatchY + row * SLOT_SIZE;
            context.fill(swatchX, yOffset, swatchX + SLOT_INNER_SIZE, yOffset + SLOT_INNER_SIZE, 0xFF000000 | color);
            drawSwatchLabel(context, direction, swatchX, yOffset);
        }
    }

    private void drawSwatchLabel(
            GuiGraphics context, net.minecraft.core.Direction direction, int swatchX, int swatchY) {
        String label =
                switch (direction) {
                    case NORTH -> "N";
                    case SOUTH -> "S";
                    case EAST -> "E";
                    case WEST -> "W";
                    case UP -> "U";
                    case DOWN -> "D";
                };
        int textWidth = font.width(label);
        int textX = swatchX + (SLOT_INNER_SIZE - textWidth) / 2;
        int textY = swatchY + (SLOT_INNER_SIZE - font.lineHeight) / 2;
        context.drawString(font, label, textX, textY, 0xF0FFFFFF, true);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }
}
