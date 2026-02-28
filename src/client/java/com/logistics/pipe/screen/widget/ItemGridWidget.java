package com.logistics.pipe.screen.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Scrollable grid widget displaying network items.
 * Shows items in 8 columns with vertical scrolling.
 */
public class ItemGridWidget {
    private static final int COLUMNS = 8;
    private static final int VISIBLE_ROWS = 5;
    private static final int SLOT_SIZE = 20; // 18px item + 2px padding
    private static final int SCROLL_BAR_WIDTH = 6;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private List<ItemStack> items = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private final Consumer<ItemStack> onItemSelected;

    public ItemGridWidget(int x, int y, int width, int height, Consumer<ItemStack> onItemSelected) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onItemSelected = onItemSelected;
    }

    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>(items);
        this.scrollOffset = 0;
        this.selectedIndex = -1;
        this.hoveredIndex = -1;
    }

    public void setSelectedItem(ItemStack stack) {
        this.selectedIndex = -1;
        if (stack == null || stack.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            if (ItemStack.isSameItemSameComponents(items.get(i), stack)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Reset hovered index
        hoveredIndex = -1;

        // Render items in grid
        int totalSlots = COLUMNS * VISIBLE_ROWS;
        int startIndex = scrollOffset * COLUMNS;

        for (int i = 0; i < totalSlots; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) {
                break;
            }

            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int slotX = x + column * SLOT_SIZE;
            int slotY = y + row * SLOT_SIZE;

            // Check if hovered
            boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            if (hovered) {
                hoveredIndex = itemIndex;
            }

            // Draw slot background
            if (itemIndex == selectedIndex) {
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737);
            } else if (hovered) {
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF4B4B4B);
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF000000);
            } else {
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            }

            // Render item
            ItemStack stack = items.get(itemIndex);
            graphics.renderItem(stack, slotX + 1, slotY + 1);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, slotX + 1, slotY + 1);
        }

        // Render scroll bar if needed
        int totalRows = (int) Math.ceil(items.size() / (double) COLUMNS);
        if (totalRows > VISIBLE_ROWS) {
            renderScrollBar(graphics, totalRows);
        }
    }

    private void renderScrollBar(GuiGraphics graphics, int totalRows) {
        int scrollBarX = x + width - SCROLL_BAR_WIDTH;
        int scrollBarHeight = height;
        int scrollThumbHeight = Math.max(10, scrollBarHeight * VISIBLE_ROWS / totalRows);

        // Background
        graphics.fill(scrollBarX, y, scrollBarX + SCROLL_BAR_WIDTH, y + scrollBarHeight, 0xFF8B8B8B);

        // Thumb
        int maxScroll = totalRows - VISIBLE_ROWS;
        int thumbY = y + (scrollBarHeight - scrollThumbHeight) * scrollOffset / maxScroll;
        graphics.fill(
                scrollBarX,
                thumbY,
                scrollBarX + SCROLL_BAR_WIDTH,
                thumbY + scrollThumbHeight,
                0xFFFFFFFF
        );
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicked on an item
        int totalSlots = COLUMNS * VISIBLE_ROWS;
        int startIndex = scrollOffset * COLUMNS;

        for (int i = 0; i < totalSlots; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) {
                break;
            }

            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int slotX = x + column * SLOT_SIZE;
            int slotY = y + row * SLOT_SIZE;

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                selectedIndex = itemIndex;
                onItemSelected.accept(items.get(itemIndex));
                return true;
            }
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (int) Math.ceil(items.size() / (double) COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        if (scrollY < 0) {
            scrollOffset = Math.min(scrollOffset + 1, maxScroll);
        } else if (scrollY > 0) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
        }

        return true;
    }

    public int getHoveredIndex() {
        return hoveredIndex;
    }

    public ItemStack getHoveredItem() {
        if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
            return items.get(hoveredIndex);
        }
        return ItemStack.EMPTY;
    }
}
