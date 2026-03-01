package com.logistics.pipe.screen;

import com.logistics.pipe.network.RequestItemPacket;
import com.logistics.pipe.screen.widget.ItemGridWidget;
import com.logistics.pipe.ui.RequesterScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client-side screen for the Requester GUI.
 * Displays a scrollable grid of network items with search and request functionality.
 */
public class RequesterScreen extends AbstractContainerScreen<RequesterScreenHandler> {
    private ItemGridWidget itemGrid;
    private EditBox searchField;
    private EditBox amountField;
    private Button requestButton;
    private ItemStack selectedItem = ItemStack.EMPTY;

    public RequesterScreen(RequesterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Item grid (8×5 grid at 20px slots = 160x100)
        this.itemGrid = new ItemGridWidget(leftPos + 8, topPos + 20, 160, 100, this::onItemSelected);
        addRenderableWidget(itemGrid);

        // Search field
        this.searchField = new EditBox(
                font,
                leftPos + 8,
                topPos + 125,
                140,
                12,
                Component.translatable("gui.logistics.requester.search")
        );
        searchField.setHint(Component.translatable("gui.logistics.requester.search"));
        searchField.setMaxLength(50);
        searchField.setResponder(this::onSearchChanged);
        addRenderableWidget(searchField);

        // Amount field
        this.amountField = new EditBox(
                font,
                leftPos + 8,
                topPos + 142,
                50,
                16,
                Component.literal("Amount")
        );
        amountField.setValue("64");
        amountField.setMaxLength(6);
        addRenderableWidget(amountField);

        // Request button
        this.requestButton = Button.builder(
                        Component.translatable("gui.logistics.requester.request"),
                        btn -> onRequestClick())
                .bounds(leftPos + 112, topPos + 142, 56, 16)
                .build();
        addRenderableWidget(requestButton);

        // Load items from screen handler
        refreshItems();
    }

    private void refreshItems() {
        itemGrid.setItems(getMenu().getAllItems());
    }

    private void onItemSelected(ItemStack stack) {
        selectedItem = stack;
        itemGrid.setSelectedItem(stack);

        // Auto-fill amount with available quantity (capped at 64)
        long available = getMenu().getAvailableAmount(stack);
        amountField.setValue(String.valueOf(Math.min(available, 64)));

        // Update button state
        updateRequestButton();
    }

    private void onSearchChanged(String search) {
        itemGrid.setItems(getMenu().getFilteredItems(search));
        selectedItem = ItemStack.EMPTY;
        updateRequestButton();
    }

    private void onRequestClick() {
        int amount = getAmount();
        if (!selectedItem.isEmpty() && amount > 0) {
            // Send packet to server
            ClientPlayNetworking.send(new RequestItemPacket(
                    getMenu().getPipePos(),
                    selectedItem,
                    amount
            ));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private int getAmount() {
        try {
            return Integer.parseInt(amountField.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateRequestButton() {
        requestButton.active = !selectedItem.isEmpty() && getAmount() > 0;
    }

    /**
     * Called by packet receiver to update available items from server.
     */
    public void updateAvailableItems(List<ItemStack> items, List<Long> amounts) {
        getMenu().setAvailableItems(items, amounts);
        String currentSearch = searchField.getValue();
        if (currentSearch.isEmpty()) {
            itemGrid.setItems(items);
        } else {
            itemGrid.setItems(getMenu().getFilteredItems(currentSearch));
        }
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        // Draw simple gray background
        int bgColor = 0xFFC6C6C6;
        context.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, bgColor);

        // Draw darker border
        int borderColor = 0xFF8B8B8B;
        context.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, borderColor);
        context.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, borderColor);
        context.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, borderColor);
        context.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, borderColor);

        // Item grid renders itself as a widget
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        // Draw "Amount:" label
        Component amountLabel = Component.translatable("gui.logistics.requester.amount");
        context.drawString(font, amountLabel, 8, 130, 0x404040, false);
    }
}
