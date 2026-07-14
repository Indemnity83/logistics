package com.logistics.test.ui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;

public class TestMachineScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData data;

    public TestMachineScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(4), new SimpleContainerData(10));
    }

    public TestMachineScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(net.minecraft.world.inventory.MenuType.items(), syncId); // dummy menu type
        this.inventory = inventory;
        this.data = data;

        // Machine slots (0-3)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(inventory, i, 10 + (i % 2) * 18, 10 + (i / 2) * 18));
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.getSlot(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (!this.moveItemStackTo(stack, 4, 44, true)) {
                return ItemStack.EMPTY;
            } else if (stack.isEmpty()) {
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
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return this.inventory.stillValid(player);
    }
}
