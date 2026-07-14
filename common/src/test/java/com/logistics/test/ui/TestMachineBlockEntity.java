package com.logistics.test.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.HolderLookup;

/**
 * A test BlockEntity with 4 slots (0, 1, 2, 3).
 */
public class TestMachineBlockEntity extends BlockEntity {

    public static final int TOTAL_SLOTS = 4;

    private final Container inventory;
    private final ContainerData data;

    public TestMachineBlockEntity(BlockPos pos, BlockState state) {
        super(TestMachineBlockEntityType.TEST_MACHINE, pos, state);
        this.inventory = new net.minecraft.world.SimpleContainer(TOTAL_SLOTS);
        this.data = new net.minecraft.world.inventory.SimpleContainerData(10);
    }

    public Container getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }
}
