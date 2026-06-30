package com.logistics.automation.alloysmelter;

import com.logistics.LogisticsAutomation;
import java.util.List;
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

/**
 * Screen handler for the Alloy Smelter GUI.
 * Two input slots plus primary and secondary (byproduct) output slots, with progress + energy
 * synced to the client.
 */
public class AlloySmelterScreenHandler extends RecipeBookMenu {

    private static final int MACHINE_SLOT_COUNT = AlloySmelterBlockEntity.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public AlloySmelterScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(AlloySmelterBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public AlloySmelterScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.ALLOY_SMELTER, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, AlloySmelterBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Positions matched to textures/gui/automation/alloy_smelter.png.
        // Inputs (0, 1).
        this.addSlot(new Slot(inventory, AlloySmelterBlockEntity.INPUT_A_SLOT, 58, 35));
        this.addSlot(new Slot(inventory, AlloySmelterBlockEntity.INPUT_B_SLOT, 76, 35));
        // Primary output (2) and secondary byproduct (3) — extraction only.
        this.addSlot(new OutputSlot(inventory, AlloySmelterBlockEntity.PRIMARY_OUTPUT_SLOT, 132, 21));
        this.addSlot(new OutputSlot(inventory, AlloySmelterBlockEntity.SECONDARY_OUTPUT_SLOT, 136, 51));

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

    // ==================== RecipeBookMenu ====================

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.FURNACE;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        ItemStack inputA = inventory.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT);
        if (!inputA.isEmpty()) {
            contents.accountSimpleStack(inputA);
        }
        ItemStack inputB = inventory.getItem(AlloySmelterBlockEntity.INPUT_B_SLOT);
        if (!inputB.isEmpty()) {
            contents.accountSimpleStack(inputB);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PostPlaceAction handlePlacement(boolean placeAll, boolean isCreative, RecipeHolder<?> recipe, ServerLevel level, Inventory playerInventory) {
        List<Slot> relevant = List.of(
                this.getSlot(AlloySmelterBlockEntity.INPUT_A_SLOT),
                this.getSlot(AlloySmelterBlockEntity.INPUT_B_SLOT));
        return ServerPlaceRecipe.placeRecipe(
            new ServerPlaceRecipe.CraftingMenuAccess<AlloySmelterRecipe>() {
                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents contents) {
                    AlloySmelterScreenHandler.this.fillCraftSlotsStackedContents(contents);
                }

                @Override
                public void clearCraftingContent() {
                    relevant.forEach(s -> s.set(ItemStack.EMPTY));
                }

                @Override
                public boolean recipeMatches(RecipeHolder<AlloySmelterRecipe> r) {
                    return r.value().matches(new DualRecipeInput(
                            inventory.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT),
                            inventory.getItem(AlloySmelterBlockEntity.INPUT_B_SLOT)), level);
                }
            },
            2, 1,
            relevant,
            relevant,
            playerInventory,
            (RecipeHolder<AlloySmelterRecipe>) recipe,
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

            if (index < PLAYER_INVENTORY_START) {
                // Machine slot -> player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, AlloySmelterBlockEntity.INPUT_A_SLOT, AlloySmelterBlockEntity.INPUT_B_SLOT + 1, false)) {
                // Player inventory -> input slots
                return ItemStack.EMPTY;
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }

    // ==================== Data Getters for GUI Rendering ====================

    public int getProcessProgress() {
        return data.get(0);
    }

    public int getProcessTotalTicks() {
        return data.get(1);
    }

    public int getEnergyStored() {
        return data.get(2);
    }

    public int getEnergyCapacity() {
        return (int) AlloySmelterBlockEntity.ENERGY_CAPACITY;
    }

    public int getProgressArrowWidth() {
        int total = getProcessTotalTicks();
        if (total <= 0) return 0;
        return 24 * getProcessProgress() / total;
    }

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
