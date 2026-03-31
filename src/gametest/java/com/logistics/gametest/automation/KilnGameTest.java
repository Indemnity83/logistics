package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class KilnGameTest {

    /**
     * Test that kiln can be placed and creates block entity.
     */
    @GameTest
    public void testKilnPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);

        KilnBlockEntity blockEntity = context.getBlockEntity(pos, KilnBlockEntity.class);
        if (blockEntity == null) {
            context.fail("Kiln should create KilnBlockEntity");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln inventory is accessible from correct sides.
     * Top/Sides: input, Bottom: output extraction.
     */
    @GameTest
    public void testKilnInventoryAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Kiln should implement WorldlyContainer");
            return;
        }

        // Slot layout: 0 = input, 1 = output
        final int inputSlot = 0;
        final int outputSlot = 1;

        // Top: should expose input slot
        int[] topSlots = sidedInv.getSlotsForFace(Direction.UP);
        if (topSlots.length != 1 || topSlots[0] != inputSlot) {
            context.fail("Top should expose input slot (" + inputSlot + "), got: "
                + java.util.Arrays.toString(topSlots));
            return;
        }

        // Bottom: should expose output slot
        int[] bottomSlots = sidedInv.getSlotsForFace(Direction.DOWN);
        if (bottomSlots.length != 1 || bottomSlots[0] != outputSlot) {
            context.fail("Bottom should expose output slot (" + outputSlot + "), got: "
                + java.util.Arrays.toString(bottomSlots));
            return;
        }

        // Sides: should expose no slots
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int[] sideSlots = sidedInv.getSlotsForFace(side);
            if (sideSlots.length != 0) {
                context.fail(side + " should expose no slots, got: " + java.util.Arrays.toString(sideSlots));
                return;
            }
        }

        context.succeed();
    }

    /**
     * Test that kiln allows input insertion from top only, not from sides or bottom.
     */
    @GameTest
    public void testKilnInputAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        final int inputSlot = 0;
        ItemStack ironOre = new ItemStack(Items.IRON_ORE);

        // Top: should allow insertion into input slot
        if (!sidedInv.canPlaceItemThroughFace(inputSlot, ironOre, Direction.UP)) {
            context.fail("Kiln should accept items from top into input slot");
            return;
        }

        // Horizontal sides: should NOT allow insertion
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (sidedInv.canPlaceItemThroughFace(inputSlot, ironOre, side)) {
                context.fail("Kiln should NOT accept items from " + side);
                return;
            }
        }

        // Bottom: should NOT allow insertion
        if (sidedInv.canPlaceItemThroughFace(inputSlot, ironOre, Direction.DOWN)) {
            context.fail("Kiln should NOT accept items from bottom");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln only allows output extraction from bottom.
     */
    @GameTest
    public void testKilnOutputExtraction(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        final int outputSlot = 1;
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);

        // Bottom should allow extraction from output slot
        if (!sidedInv.canTakeItemThroughFace(outputSlot, ironIngot, Direction.DOWN)) {
            context.fail("Kiln should allow output extraction from bottom");
            return;
        }

        // Top should NOT allow extraction
        if (sidedInv.canTakeItemThroughFace(outputSlot, ironIngot, Direction.UP)) {
            context.fail("Kiln should NOT allow extraction from top");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln has correct initial state.
     */
    @GameTest
    public void testKilnInitialState(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        // Verify LIT state is false
        BlockState state = context.getBlockState(pos);
        if (state.getValue(KilnBlock.LIT)) {
            context.fail("Newly placed kiln should not be lit");
            return;
        }

        // Verify zero energy initially
        if (kiln.energyStorage(null).getAmount() != 0) {
            context.fail("Newly placed kiln should have zero energy, got: " + kiln.energyStorage(null).getAmount());
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln provides item storage capability.
     */
    @GameTest
    public void testKilnItemStorage(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        for (Direction direction : Direction.values()) {
            Storage<ItemVariant> storage = kiln.itemStorage(direction);
            if (storage == null) {
                context.fail("Kiln should provide item storage from " + direction);
                return;
            }
        }

        context.succeed();
    }

    /**
     * Test that kiln block state has correct FACING property.
     */
    @GameTest
    public void testKilnFacing(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        BlockState state = context.getBlockState(pos);

        if (!state.hasProperty(KilnBlock.FACING)) {
            context.fail("Kiln should have FACING property");
            return;
        }

        Direction facing = state.getValue(KilnBlock.FACING);
        if (facing == Direction.UP || facing == Direction.DOWN) {
            context.fail("Kiln FACING should be horizontal, got: " + facing);
            return;
        }

        context.succeed();
    }
}
