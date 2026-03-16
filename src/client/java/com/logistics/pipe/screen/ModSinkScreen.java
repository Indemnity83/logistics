package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.ModSinkScreenHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side screen for the Mod-Based Item Sink GUI.
 *
 * <p>Layout (coordinates relative to background top-left):
 * <ul>
 *   <li>(8, 19): preview item slot — place an item to identify its mod</li>
 *   <li>(28, 24): mod name display for the preview item</li>
 *   <li>(148, 17): "Add" button (22 × 20)</li>
 *   <li>(8, 40) to (167, 86): scrollable list of active mod filters with remove buttons</li>
 *   <li>(8, 102): player inventory (3 × 9)</li>
 *   <li>(8, 160): hotbar (1 × 9)</li>
 * </ul>
 */
public class ModSinkScreen extends AbstractContainerScreen<ModSinkScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/mod_sink.png");

    // List display constants (relative to background)
    private static final int LIST_X = 8;
    private static final int LIST_Y = 40;
    private static final int LIST_WIDTH = 159;
    private static final int LIST_HEIGHT = 46;
    private static final int ENTRY_HEIGHT = 14;
    private static final int ENTRIES_VISIBLE = LIST_HEIGHT / ENTRY_HEIGHT; // 3
    private static final int REMOVE_BTN_WIDTH = 10;
    private static final int REMOVE_BTN_X_OFFSET = LIST_WIDTH - REMOVE_BTN_WIDTH - 2; // 147
    private static final int TEXT_COLOR = 0xFF404040;

    private int scrollOffset = 0;

    public ModSinkScreen(ModSinkScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.titleLabelY = 6;
        this.inventoryLabelY = 91;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("gui.logistics.mod_sink.add"),
                btn -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                })
                .bounds(leftPos + 148, topPos + 17, 22, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);

        // Mod name for preview slot
        ItemStack preview = menu.getSlot(ModSinkScreenHandler.PREVIEW_SLOT).getItem();
        if (!preview.isEmpty()) {
            String namespace = BuiltInRegistries.ITEM.getKey(preview.getItem()).getNamespace();
            String modName = getModName(namespace);
            int maxWidth = imageWidth - 28 - 22 - 6; // space before "Add" button
            String truncated = font.plainSubstrByWidth(modName, maxWidth);
            graphics.drawString(font, truncated, 28, 24, TEXT_COLOR, false);
        }

        renderFilterList(graphics, mouseX, mouseY);
    }

    private void renderFilterList(GuiGraphics graphics, int mouseX, int mouseY) {
        int filterCount = menu.getFilterCount();
        int maxScroll = Math.max(0, filterCount - ENTRIES_VISIBLE);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < ENTRIES_VISIBLE; i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex >= filterCount) break;

            ItemStack stack = menu.getSlot(ModSinkScreenHandler.FILTER_SLOT_START + entryIndex).getItem();
            if (stack.isEmpty()) break;

            int entryY = LIST_Y + i * ENTRY_HEIGHT;
            String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
            String modName = getModName(namespace);

            // Mod name text (leave room for the item icon and remove button)
            int maxTextWidth = LIST_WIDTH - 20 - REMOVE_BTN_WIDTH - 6;
            String truncated = font.plainSubstrByWidth(modName, maxTextWidth);
            graphics.drawString(font, truncated, LIST_X + 20, entryY + 3, 0xFF8B8B8B, false);

            // Remove button "×"
            boolean hoveringRemove = isOverRemoveButton(i, mouseX + leftPos, mouseY + topPos);
            int removeColor = hoveringRemove ? 0xFFFFFFFF : 0xFF888888;
            graphics.drawString(font, "X", LIST_X + REMOVE_BTN_X_OFFSET + 1, entryY + 3, removeColor, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFilterItems(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** Render item icons for visible filter list entries (needs absolute coords). */
    private void renderFilterItems(GuiGraphics graphics) {
        int filterCount = menu.getFilterCount();
        for (int i = 0; i < ENTRIES_VISIBLE; i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex >= filterCount) break;

            ItemStack stack = menu.getSlot(ModSinkScreenHandler.FILTER_SLOT_START + entryIndex).getItem();
            if (stack.isEmpty()) break;

            int entryY = topPos + LIST_Y + i * ENTRY_HEIGHT;
            // Render at 12×12 by using a scaled blit, but renderItem is always 16×16.
            // Use renderItem with a scissor so it fits neatly within the 14px row.
            graphics.renderItem(stack, leftPos + LIST_X + 2, entryY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInListArea(mouseX, mouseY)) {
            int filterCount = menu.getFilterCount();
            int maxScroll = Math.max(0, filterCount - ENTRIES_VISIBLE);
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() == 0) {
            int filterCount = menu.getFilterCount();
            for (int i = 0; i < ENTRIES_VISIBLE; i++) {
                int entryIndex = i + scrollOffset;
                if (entryIndex >= filterCount) break;
                if (isOverRemoveButton(i, event.x(), event.y())) {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, entryIndex + 1);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    private boolean isInListArea(double mouseX, double mouseY) {
        int absX = leftPos + LIST_X;
        int absY = topPos + LIST_Y;
        return mouseX >= absX && mouseX <= absX + LIST_WIDTH
                && mouseY >= absY && mouseY <= absY + LIST_HEIGHT;
    }

    private boolean isOverRemoveButton(int visibleIndex, double mouseX, double mouseY) {
        int btnAbsX = leftPos + LIST_X + REMOVE_BTN_X_OFFSET;
        int btnAbsY = topPos + LIST_Y + visibleIndex * ENTRY_HEIGHT;
        return mouseX >= btnAbsX && mouseX <= btnAbsX + REMOVE_BTN_WIDTH
                && mouseY >= btnAbsY && mouseY <= btnAbsY + ENTRY_HEIGHT;
    }

    private static String getModName(String namespace) {
        return FabricLoader.getInstance()
                .getModContainer(namespace)
                .map(c -> c.getMetadata().getName())
                .orElse(namespace);
    }
}
