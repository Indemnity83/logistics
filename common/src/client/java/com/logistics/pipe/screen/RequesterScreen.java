package com.logistics.pipe.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.network.packet.RequestItemPacket;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.screen.widget.NetworkItemButton;
import com.logistics.pipe.screen.widget.PageButton;
import com.logistics.pipe.ui.RequesterScreenHandler;
import com.logistics.core.lib.platform.ClientNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Book-style requester screen with compact 5×4 grid, search, and pagination.
 * Uses requester-alt.png background texture (147×166 pixels).
 */
public class RequesterScreen extends AbstractContainerScreen<RequesterScreenHandler> {
    private static final ResourceId BACKGROUND =
            LogisticsMod.modId("textures/gui/pipe/requester-alt.png");
    private static final int MAX_REQUEST_CAP = RequesterModule.MAX_REQUEST_AMOUNT;

    private EditBox searchBox;
    private EditBox amountField;
    private Button requestButton;
    private PageButton prevPageButton;
    private PageButton nextPageButton;

    private final List<NetworkItemButton> itemButtons = new ArrayList<>();
    private NetworkItemButton selectedButton = null;

    public RequesterScreen(RequesterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 222;
        this.imageHeight = 183;
    }

    @Override
    protected void init() {
        super.init();

        // Search box at top
        this.searchBox = new EditBox(
                this.font,
                this.leftPos + 27,
                this.topPos + 14,
                183,
                14,
                Component.translatable("gui.logistics.requester.search")
        );
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.translatable("gui.logistics.requester.search"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // Item button grid (5×4 = 20 buttons)
        itemButtons.clear();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 8; col++) {
                int x = leftPos + 12 + col * 25; // 25px slot, no spacing
                int y = topPos + 32 + row * 25;
                NetworkItemButton button = new NetworkItemButton(x, y, this::onItemClicked);
                itemButtons.add(button);
                addRenderableWidget(button);
            }
        }

        // Pagination buttons (12×17 pixels)
        prevPageButton = new PageButton(
                leftPos + 74,
                topPos + 137,
                false, // backward
                this::previousPage
        );
        addRenderableWidget(prevPageButton);

        nextPageButton = new PageButton(
                leftPos + 136,
                topPos + 137,
                true, // forward
                this::nextPage
        );
        addRenderableWidget(nextPageButton);

        Button decrementButton = Button.builder(
                Component.literal("-"),
                this::subOne)
            .bounds(leftPos + 14, topPos + 163, 14, 14)
            .build();
        addRenderableWidget(decrementButton);

        // Amount field
        amountField = new EditBox(
                this.font,
                leftPos + 30,
                topPos + 163,
                30,
                14,
                Component.literal("Amount")
        );
        amountField.setValue("1");
        amountField.setMaxLength(4);
        // MC 1.21.1: setCentered() not available
        amountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        addRenderableWidget(amountField);

        Button incrementButton = Button.builder(
                Component.literal("+"),
                this::addOne)
            .bounds(leftPos + 64, topPos + 163, 14, 14)
            .build();
        addRenderableWidget(incrementButton);

        // Request button
        requestButton = Button.builder(
                        Component.translatable("gui.logistics.requester.request"),
                        this::onRequestClick)
                .bounds(leftPos + 155, topPos + 162, 60, 16)
                .build();
        addRenderableWidget(requestButton);

        // Load initial items
        getMenu().refreshSearch("");
        refreshItems();
    }

    private void subOne(Button button) {
        try {
            int currentAmount = Math.min(MAX_REQUEST_CAP, Integer.parseInt(amountField.getValue()));
            amountField.setValue(String.valueOf(Math.max(1, currentAmount - 1)));
        } catch (NumberFormatException e) {
            amountField.setValue("1");
        }
    }

    private void addOne(Button button) {
        try {
            int currentAmount = Integer.parseInt(amountField.getValue());
            amountField.setValue(String.valueOf(Math.min(MAX_REQUEST_CAP, currentAmount + 1)));
        } catch (NumberFormatException e) {
            amountField.setValue("1");
        }
    }

    private void onSearchChanged(String query) {
        getMenu().refreshSearch(query);
        selectedButton = null; // Clear selection when search changes
        refreshItems();
    }

    private void refreshItems() {
        List<ItemStack> pageItems = getMenu().getCurrentPageItems();

        // Update button display
        for (int i = 0; i < itemButtons.size(); i++) {
            NetworkItemButton button = itemButtons.get(i);
            if (i < pageItems.size()) {
                ItemStack stack = pageItems.get(i);
                long amount = getMenu().getAvailableAmount(stack);
                button.setItem(stack, amount);
                button.visible = true;

                // Restore selection if same item is on current page
                if (selectedButton != null &&
                        ItemStack.isSameItemSameComponents(selectedButton.getItem(), stack)) {
                    selectedButton = button;
                    button.setSelected(true);
                } else {
                    button.setSelected(false);
                }
            } else {
                button.setItem(ItemStack.EMPTY, 0);
                button.setSelected(false);
                button.visible = false;
            }
        }

        // Update pagination buttons
        boolean hasMultiplePages = getMenu().getTotalPages() > 1;
        prevPageButton.visible = hasMultiplePages;
        nextPageButton.visible = hasMultiplePages;
        prevPageButton.active = getMenu().getCurrentPage() > 0;
        nextPageButton.active = getMenu().getCurrentPage() < getMenu().getTotalPages() - 1;

        // Update request button
        updateRequestButton();
    }

    private void onItemClicked(NetworkItemButton button) {
        // Deselect previous
        if (selectedButton != null) {
            selectedButton.setSelected(false);
        }

        // Select new
        selectedButton = button;
        button.setSelected(true);

        updateRequestButton();
    }

    private void previousPage() {
        getMenu().previousPage();
        refreshItems();
    }

    private void nextPage() {
        getMenu().nextPage();
        refreshItems();
    }

    private void onRequestClick(Button button) {
        if (selectedButton != null && !selectedButton.getItem().isEmpty()) {
            try {
                int amount = Math.min(MAX_REQUEST_CAP, Math.max(1, Integer.parseInt(amountField.getValue())));
                amountField.setValue(String.valueOf(amount)); // reflect any clamping visibly
                if (amount > 0) {
                    // Normalize the ItemStack to count=1 (actual amount is sent separately)
                    ItemStack requestStack = selectedButton.getItem().copy();
                    requestStack.setCount(1);

                    ClientNetworking.send(new RequestItemPacket(
                            getMenu().getPipePos(),
                            requestStack,
                            amount
                    ));
                    onClose();
                }
            } catch (NumberFormatException e) {
                // Invalid amount - do nothing
            }
        }
    }

    private void updateRequestButton() {
        boolean hasSelection = selectedButton != null && !selectedButton.getItem().isEmpty();
        boolean hasValidAmount = false;
        try {
            int amount = Integer.parseInt(amountField.getValue());
            hasValidAmount = amount > 0;
        } catch (NumberFormatException e) {
            // Invalid amount
        }
        requestButton.active = hasSelection && hasValidAmount;
    }

    /**
     * Called by the loader packet receivers. Payloads addressed to a different menu are ignored, so a
     * sync for another player's requester can never replace this screen's contents.
     */
    public void applySync(SyncRequesterInventoryPacket packet) {
        if (!getMenu().applySync(packet)) {
            return;
        }
        getMenu().refreshSearch(searchBox.getValue());
        refreshItems();
    }

    // MC 1.21.1: keyPressed uses (int, int, int) instead of KeyEvent
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != InputConstants.KEY_ESCAPE) {
            if (searchBox.isFocused()) {
                searchBox.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
            if (amountField.isFocused()) {
                amountField.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw requester-alt.png background
        // MC 1.21.1: blit() without RenderPipeline param
        graphics.blit(
                BACKGROUND.toIdentifier(),
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256
        );

        // Draw page indicator if multiple pages
        if (getMenu().getTotalPages() > 0) {
            String pageText = (getMenu().getCurrentPage() + 1) + "/" + getMenu().getTotalPages();
            int textWidth = font.width(pageText);
            graphics.drawString(
                    font,
                    pageText,
                    leftPos + (imageWidth - textWidth) / 2,
                    topPos + imageHeight - 57,
                    0x404040,
                    false
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is rendered by texture, no need for text overlay
        // Amount label
        Component amountLabel = Component.translatable("gui.logistics.requester.amount");
        graphics.drawString(font, amountLabel, 8, imageHeight - 52, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Render item tooltips
        for (NetworkItemButton button : itemButtons) {
            if (button.isHoveredOrFocused() && !button.getItem().isEmpty()) {
                graphics.renderTooltip(this.font, button.getItem(), mouseX, mouseY);
                break;
            }
        }
    }
}
