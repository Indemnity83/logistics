package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.block.MachineResultSlot;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

/**
 * Screen handler for the Iron Macerator GUI.
 * Manages input/output slots and syncs progress/energy data to the client.
 */
public class MaceratorScreenHandler extends RecipeBookMenu {

    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36; // 27 + 9 hotbar

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(MaceratorBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.MACERATOR, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MaceratorBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Slot positions match the Sawmill GUI. Input (0) on the left.
        this.addSlot(new Slot(inventory, MaceratorBlockEntity.INPUT_SLOT, 56, 35));

        // Primary output (1, upper right) — no insertion; releases banked maceration XP to the player on take
        this.addSlot(new MachineResultSlot(inventory, MaceratorBlockEntity.OUTPUT_SLOT, 116, 25));

        // Secondary chance byproduct (2, lower right) — extraction only, no XP
        this.addSlot(new OutputSlot(inventory, MaceratorBlockEntity.SECONDARY_OUTPUT_SLOT, 116, 51));

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.FURNACE;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        ItemStack input = inventory.getItem(MaceratorBlockEntity.INPUT_SLOT);
        if (!input.isEmpty()) {
            contents.accountSimpleStack(input);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PostPlaceAction handlePlacement(boolean placeAll, boolean isCreative, RecipeHolder<?> recipe, ServerLevel level, Inventory playerInventory) {
        List<Slot> relevant = List.of(this.getSlot(0));
        return ServerPlaceRecipe.placeRecipe(
            new ServerPlaceRecipe.CraftingMenuAccess<MaceratorRecipeWrapper>() {
                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents contents) {
                    MaceratorScreenHandler.this.fillCraftSlotsStackedContents(contents);
                }

                @Override
                public void clearCraftingContent() {
                    relevant.forEach(s -> s.set(ItemStack.EMPTY));
                }

                @Override
                public boolean recipeMatches(RecipeHolder<MaceratorRecipeWrapper> r) {
                    return r.value().matches(new SingleRecipeInput(inventory.getItem(MaceratorBlockEntity.INPUT_SLOT)), level);
                }
            },
            1, 1,
            List.of(this.getSlot(0)),
            relevant,
            playerInventory,
            (RecipeHolder<MaceratorRecipeWrapper>) recipe,
            placeAll,
            isCreative
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == MaceratorBlockEntity.OUTPUT_SLOT) {
                // Move output to player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index >= PLAYER_INVENTORY_START) {
                // Move from player inventory to input slot
                if (!this.moveItemStackTo(stack, MaceratorBlockEntity.INPUT_SLOT, MaceratorBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from machine to player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    // ==================== Data Getters for GUI Rendering ====================

    /** Energy spent so far toward the active recipe (RF). */
    public int getEnergySpent() {
        return data.get(MaceratorBlockEntity.DATA_PROGRESS);
    }

    /** Total energy the active recipe requires (RF), or 0 if idle. */
    public int getEnergyRequired() {
        return data.get(MaceratorBlockEntity.DATA_TOTAL);
    }

    /** Current energy stored (RF). */
    public int getEnergyStored() {
        return data.get(MaceratorBlockEntity.DATA_ENERGY);
    }

    /** Max energy capacity (RF). */
    public int getEnergyCapacity() {
        return (int) MaceratorBlockEntity.ENERGY_CAPACITY;
    }

    /** Processing progress as a 0–25 pixel width for the arrow (the sprite width), or 0 if idle. */
    public int getProgressArrowWidth() {
        int required = getEnergyRequired();
        if (required <= 0) return 0;
        return 24 * getEnergySpent() / required;
    }

    /** Energy bar height in pixels (0–13). */
    public int getEnergyBarHeight() {
        int capacity = getEnergyCapacity();
        if (capacity <= 0) return 0;
        return 30 * getEnergyStored() / capacity;
    }

    /** Output slot — players may take but not insert. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
