package com.logistics.pipe.screen.widget;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
            graphics.item(item, getX() + 4, getY() + 4);
            graphics.itemDecorations(
                    Minecraft.getInstance().font,
                    item,
                    getX() + 4,
                    getY() + 4,
                    formatAmount(availableAmount)
            );
        }
    }

    private String formatAmount(long amount) {
        if (amount >= 1_000_000) {
            long millions = amount / 1_000_000;
            long tenths = (amount % 1_000_000) / 100_000;
            return tenths > 0 ? millions + "." + tenths + "M" : millions + "M";
        } else if (amount >= 1_000) {
            long thousands = amount / 1_000;
            long tenths = (amount % 1_000) / 100;
            return tenths > 0 ? thousands + "." + tenths + "K" : thousands + "K";
        }
        return String.valueOf(amount);
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
