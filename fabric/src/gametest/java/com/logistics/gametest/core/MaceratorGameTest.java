package com.logistics.gametest.core;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-world tests for the component-hosted Macerator: capability routing, furnace-style sided
 * access, and an end-to-end RF-cost maceration run (the path that exercises the live recipe
 * manager, energy spend, and output production through the machine framework).
 */
public class MaceratorGameTest {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int SECONDARY_OUTPUT_SLOT = 2;

    @GameTest
    public void testPlacementCreatesBlockEntity(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.MACERATOR);

        if (context.getBlockEntity(pos, MaceratorBlockEntity.class) == null) {
            context.fail("Macerator should create MaceratorBlockEntity");
            return;
        }
        context.succeed();
    }

    @GameTest
    public void testSidedAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.MACERATOR);

        if (!(context.getBlockEntity(pos, MaceratorBlockEntity.class) instanceof WorldlyContainer sided)) {
            context.fail("Macerator should implement WorldlyContainer");
            return;
        }

        int[] bottom = sided.getSlotsForFace(Direction.DOWN);
        if (bottom.length != 2 || bottom[0] != OUTPUT_SLOT || bottom[1] != SECONDARY_OUTPUT_SLOT) {
            context.fail("Bottom should expose both output slots");
            return;
        }
        // Top and horizontal faces expose the input slot.
        for (Direction side : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            int[] slots = sided.getSlotsForFace(side);
            if (slots.length != 1 || slots[0] != INPUT_SLOT) {
                context.fail(side + " should expose the input slot");
                return;
            }
        }

        ItemStack ore = new ItemStack(Items.IRON_ORE);
        if (!sided.canPlaceItemThroughFace(INPUT_SLOT, ore, Direction.UP)) {
            context.fail("Input should be insertable from the top");
            return;
        }
        if (sided.canPlaceItemThroughFace(INPUT_SLOT, ore, Direction.DOWN)) {
            context.fail("Input should NOT be insertable from the bottom");
            return;
        }
        if (!sided.canTakeItemThroughFace(OUTPUT_SLOT, ItemStack.EMPTY, Direction.DOWN)) {
            context.fail("Output should be extractable from the bottom");
            return;
        }
        if (!sided.canTakeItemThroughFace(SECONDARY_OUTPUT_SLOT, ItemStack.EMPTY, Direction.DOWN)) {
            context.fail("Byproduct slot should be extractable from the bottom");
            return;
        }
        context.succeed();
    }

    @GameTest
    public void testCapabilitiesExposed(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.MACERATOR);
        MaceratorBlockEntity macerator = context.getBlockEntity(pos, MaceratorBlockEntity.class);
        if (macerator == null) {
            context.fail("Macerator block entity should exist");
            return;
        }

        if (macerator.energyStorage(null) == null) {
            context.fail("Macerator should expose energy storage");
            return;
        }
        for (Direction side : Direction.values()) {
            IItemStorage storage = macerator.itemStorage(side);
            if (storage == null) {
                context.fail("Macerator should expose item storage from " + side);
                return;
            }
        }
        if (context.getBlockState(pos).getValue(MaceratorBlock.LIT)) {
            context.fail("Newly placed macerator should not be lit");
            return;
        }
        context.succeed();
    }

    @GameTest(maxTicks = 240)
    public void testMaceratesOreWithEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.MACERATOR);
        MaceratorBlockEntity macerator = context.getBlockEntity(pos, MaceratorBlockEntity.class);
        if (macerator == null) {
            context.fail("Macerator block entity should exist");
            return;
        }

        // Fill the energy buffer (each insert is capped at the 128 RF/t input limit).
        IEnergyStorage energy = macerator.energyStorage(null);
        for (int i = 0; i < 80; i++) {
            energy.insert(128, false);
        }
        macerator.setItem(INPUT_SLOT, new ItemStack(Items.IRON_ORE));

        // iron_ore costs 2000 RF at 10 RF/t -> 200 ticks.
        context.runAfterDelay(220, () -> {
            if (!macerator.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input ore should be consumed after maceration");
                return;
            }
            ItemStack output = macerator.getItem(OUTPUT_SLOT);
            if (output.isEmpty()) {
                context.fail("Macerator should have produced dust");
                return;
            }
            context.succeed();
        });
    }
}
