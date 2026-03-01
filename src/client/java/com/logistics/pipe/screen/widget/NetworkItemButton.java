package com.logistics.pipe.screen.widget;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Item display button for the requester screen.
 * Displays an item with amount text and supports selection state.
 * Recipe Book-style 25x25 pixel button.
 */
public class NetworkItemButton extends AbstractWidget {
    private static final ResourceId SLOT_TEXTURE =
            LogisticsMod.modId("textures/gui/pipe/slot.png");
    private static final ResourceId SLOT_SELECTED_TEXTURE =
            LogisticsMod.modId("textures/gui/pipe/slot_selected.png");

    private ItemStack item = ItemStack.EMPTY;
    private long availableAmount = 0;
    private boolean selected = false;
    private final Consumer<NetworkItemButton> clickHandler;

    public NetworkItemButton(int x, int y, Consumer<NetworkItemButton> clickHandler) {
        super(x, y, 25, 25, Component.empty());
        this.clickHandler = clickHandler;
    }

    public void setItem(ItemStack item, long amount) {
        this.item = item;
        this.availableAmount = amount;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render slot background (25x25)
        ResourceId texture = selected ? SLOT_SELECTED_TEXTURE : SLOT_TEXTURE;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture.toIdentifier(),
                getX(),
                getY(),
                0,
                0,
                25,
                25,
                25,
                25
        );

        // Render item if present
        if (!item.isEmpty()) {
            // Center item in 25x25 slot (item is 16x16, so offset by 4.5 ≈ 4-5 pixels)
            graphics.renderItem(item, getX() + 4, getY() + 4);

            // Render amount text if > 1
            if (availableAmount > 1) {
                String amountText = formatAmount(availableAmount);
                graphics.drawString(
                        Minecraft.getInstance().font,
                        amountText,
                        getX() + 6,
                        getY() + 15,
                        0xFFFFFF,
                        true // with shadow
                );
            }
        }
    }

    private String formatAmount(long amount) {
        if (amount <= 64) {
            return String.valueOf(amount);
        }
        long stacks = amount / 64;
        long remainder = amount % 64;
        if (remainder > 0) {
            return stacks + "×64+" + remainder;
        } else {
            return stacks + "×64";
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (!item.isEmpty()) {
            clickHandler.accept(this);
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (!item.isEmpty()) {
            defaultButtonNarrationText(output);
        }
    }

    public ItemStack getItem() {
        return item;
    }

    public long getAvailableAmount() {
        return availableAmount;
    }
}
