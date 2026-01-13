package com.logistics.client.screen;

import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemFilterScreen extends HandledScreen<ItemFilterScreenHandler> {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_INNER_SIZE = 16;

    public ItemFilterScreen(ItemFilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 222;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);

        int swatchX = x + SLOT_START_X;
        int swatchY = y + SLOT_START_Y;
        for (int row = 0; row < ItemFilterModule.FILTER_ORDER.length; row++) {
            net.minecraft.util.math.Direction direction = ItemFilterModule.FILTER_ORDER[row];
            int color = ItemFilterModule.getFilterColor(direction);
            int yOffset = swatchY + row * SLOT_SIZE;
            context.fill(swatchX, yOffset, swatchX + SLOT_INNER_SIZE, yOffset + SLOT_INNER_SIZE, 0xFF000000 | color);
            drawSwatchLabel(context, direction, swatchX, yOffset);
        }
    }

    private void drawSwatchLabel(DrawContext context, net.minecraft.util.math.Direction direction, int swatchX, int swatchY) {
        String label = switch (direction) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            case UP -> "U";
            case DOWN -> "D";
        };
        int textWidth = textRenderer.getWidth(label);
        int textX = swatchX + (SLOT_INNER_SIZE - textWidth) / 2;
        int textY = swatchY + (SLOT_INNER_SIZE - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, label, textX, textY, 0xF0FFFFFF, true);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
    }
}
