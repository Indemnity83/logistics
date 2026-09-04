package com.logistics.automation.alloysmelter;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.Item;
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
                new SimpleContainerData(MachineData.COUNT));
    }

    /** Server-side constructor. */
    public AlloySmelterScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.ALLOY_SMELTER, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MachineData.COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Positions matched to textures/gui/automation/alloy_smelter.png.
        // Inputs (0, 1).
        this.addSlot(new Slot(inventory, AlloySmelterBlockEntity.INPUT_A_SLOT, 58, 35));
        this.addSlot(new Slot(inventory, AlloySmelterBlockEntity.INPUT_B_SLOT, 76, 35));
        // Primary output (2) and secondary byproduct (3) — extraction only.
        // Centered in the 24x24 output frame (132,21)-(155,44): item is 16x16, so inset 4px.
        this.addSlot(new OutputSlot(inventory, AlloySmelterBlockEntity.PRIMARY_OUTPUT_SLOT, 136, 25));
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
    public PostPlaceAction handlePlacement(boolean placeAll, boolean isCreative, RecipeHolder<?> recipeHolder,
            ServerLevel level, Inventory playerInventory) {
        // Vanilla ServerPlaceRecipe is a crafting-grid model (one item per slot per craft), so it can't
        // express the alloy smelter's per-input counts (e.g. 3 copper + 1 tin). Place each input's count
        // explicitly instead; shift-click fills as many whole recipes as the inputs and stacks allow.
        if (!(recipeHolder.value() instanceof AlloySmelterRecipe recipe)) {
            return PostPlaceAction.NOTHING;
        }
        Slot slotA = this.getSlot(AlloySmelterBlockEntity.INPUT_A_SLOT);
        Slot slotB = this.getSlot(AlloySmelterBlockEntity.INPUT_B_SLOT);

        // Return whatever is already in the inputs to the inventory so it can be reused, then refill.
        returnToInventory(playerInventory, slotA);
        returnToInventory(playerInventory, slotB);

        // Resolve a concrete item for each ingredient (honours tags + what the player actually holds).
        StackedItemContents available = new StackedItemContents();
        playerInventory.fillStackedContents(available);
        List<Holder<Item>> itemsUsed = new ArrayList<>();
        if (!available.canCraft(recipe, 1, itemsUsed::add) || itemsUsed.size() < 2) {
            return PostPlaceAction.PLACE_GHOST_RECIPE;
        }
        Holder<Item> itemA = itemsUsed.get(0);
        Holder<Item> itemB = itemsUsed.get(1);

        int multiples = placeAll
                ? maxMultiples(playerInventory, itemA, recipe.countA(), itemB, recipe.countB())
                : 1;
        if (multiples < 1) {
            return PostPlaceAction.PLACE_GHOST_RECIPE;
        }

        boolean filledA = fillSlot(playerInventory, slotA, itemA, recipe.countA() * multiples);
        boolean filledB = fillSlot(playerInventory, slotB, itemB, recipe.countB() * multiples);
        if (!filledA || !filledB) {
            // Not enough materials after all — roll back so we never leave a half-filled grid.
            returnToInventory(playerInventory, slotA);
            returnToInventory(playerInventory, slotB);
            return PostPlaceAction.PLACE_GHOST_RECIPE;
        }
        playerInventory.setChanged();
        return PostPlaceAction.NOTHING;
    }

    private static void returnToInventory(Inventory inventory, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            // placeItemBackInInventory disposes of the whole stack — into the inventory, or onto the
            // ground when there is no room — but leaves `copy` at full count on that drop path. Clear
            // the slot up front: writing `copy` back would leave the dropped items in the slot too.
            slot.set(ItemStack.EMPTY);
            inventory.placeItemBackInInventory(copy, false);
        }
    }

    /** Moves up to {@code count} of {@code item} from the inventory into {@code slot}; true iff all placed. */
    static boolean fillSlot(Inventory inventory, Slot slot, Holder<Item> item, int count) {
        int remaining = count;
        while (remaining > 0) {
            ItemStack current = slot.getItem();
            // Headroom, not `remaining`, bounds the take: maxMultiples sizes `count` against a whole
            // empty stack, so a slot that already holds something would otherwise be grown past its limit.
            int headroom = current.isEmpty()
                    ? new ItemStack(item.value()).getMaxStackSize()
                    : current.getMaxStackSize() - current.getCount();
            if (headroom <= 0) {
                break;
            }
            int slotId = inventory.findSlotMatchingCraftingIngredient(item, current);
            if (slotId == -1) {
                break;
            }
            ItemStack invStack = inventory.getItem(slotId);
            ItemStack taken = inventory.removeItem(slotId, Math.min(Math.min(remaining, headroom), invStack.getCount()));
            if (taken.isEmpty()) {
                break;
            }
            if (current.isEmpty()) {
                slot.set(taken);
            } else if (ItemStack.isSameItemSameComponents(current, taken)) {
                current.grow(taken.getCount());
            } else {
                // A leftover item of a different type is occupying the slot; don't merge — put it back.
                inventory.placeItemBackInInventory(taken, false);
                break;
            }
            slot.setChanged();
            remaining -= taken.getCount();
        }
        return remaining == 0;
    }

    /** Most whole recipes the inputs + per-item stack limits allow (for shift-click). */
    private static int maxMultiples(Inventory inventory, Holder<Item> itemA, int countA, Holder<Item> itemB, int countB) {
        int multiples = Math.min(
                new ItemStack(itemA.value()).getMaxStackSize() / countA,
                new ItemStack(itemB.value()).getMaxStackSize() / countB);
        if (itemA.equals(itemB)) {
            // Same item feeds both inputs — share the supply.
            multiples = Math.min(multiples, countAvailable(inventory, itemA) / (countA + countB));
        } else {
            multiples = Math.min(multiples, countAvailable(inventory, itemA) / countA);
            multiples = Math.min(multiples, countAvailable(inventory, itemB) / countB);
        }
        return Math.max(0, multiples);
    }

    private static int countAvailable(Inventory inventory, Holder<Item> item) {
        int total = 0;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (stack.is(item.value())) {
                total += stack.getCount();
            }
        }
        return total;
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

    /** Progress arrow width (0..24 px) from the synced progress fraction, or 0 if idle. */
    public int getProgressArrowWidth() {
        return MachineData.barPixels(data, MachineData.PROGRESS, 24);
    }

    /** Energy bar height (0..30 px) from the synced energy fill fraction. */
    public int getEnergyBarHeight() {
        return MachineData.barPixels(data, MachineData.ENERGY, 30);
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
