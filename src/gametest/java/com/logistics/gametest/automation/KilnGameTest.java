package com.logistics.gametest.automation;

import com.logistics.LogisticsCore;
import com.logistics.core.fabricator.KilnBlock;
import com.logistics.core.fabricator.KilnBlockEntity;
import net.minecraft.gametest.framework.GameTest;
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

    // Kiln slot indices (from KilnBlockEntity)
    private static final int SLOT_FUEL = 0;
    private static final int SLOT_GLASS_INPUT = 1;
    private static final int SLOT_OUTPUT = 11;

    /**
     * Test that kiln can be placed and creates block entity.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        // Place kiln
        context.setBlock(pos, LogisticsCore.BLOCK.KILN);

        // Verify block entity exists
        KilnBlockEntity blockEntity = (KilnBlockEntity) context.getBlockEntity(pos);
        if (blockEntity == null) {
            context.fail("Kiln should create KilnBlockEntity");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln inventory is accessible from correct sides.
     * Top: glass input, Sides: fuel, Bottom: output extraction.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnInventoryAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        // Verify kiln implements WorldlyContainer
        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Kiln should implement WorldlyContainer for sided inventory access");
            return;
        }

        // Top: should only allow glass input slot
        int[] topSlots = sidedInv.getSlotsForFace(Direction.UP);
        if (topSlots.length != 1 || topSlots[0] != SLOT_GLASS_INPUT) {
            context.fail("Top should only expose glass input slot (" + SLOT_GLASS_INPUT + "), got: "
                    + java.util.Arrays.toString(topSlots));
            return;
        }

        // Bottom: should only allow output slot
        int[] bottomSlots = sidedInv.getSlotsForFace(Direction.DOWN);
        if (bottomSlots.length != 1 || bottomSlots[0] != SLOT_OUTPUT) {
            context.fail("Bottom should only expose output slot (" + SLOT_OUTPUT + "), got: "
                    + java.util.Arrays.toString(bottomSlots));
            return;
        }

        // Sides (NORTH, SOUTH, EAST, WEST): should only allow fuel slot
        for (Direction side : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            int[] sideSlots = sidedInv.getSlotsForFace(side);
            if (sideSlots.length != 1 || sideSlots[0] != SLOT_FUEL) {
                context.fail(side + " should only expose fuel slot (" + SLOT_FUEL + "), got: "
                        + java.util.Arrays.toString(sideSlots));
                return;
            }
        }

        context.succeed();
    }

    /**
     * Test that kiln only accepts glass items from top.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnGlassInputFromTop(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        // Glass should be accepted from top into slot 1
        ItemStack glass = new ItemStack(Items.GLASS);
        if (!sidedInv.canPlaceItemThroughFace(1, glass, Direction.UP)) {
            context.fail("Kiln should accept glass from top into glass input slot");
            return;
        }

        // Glass pane should also be accepted
        ItemStack glassPane = new ItemStack(Items.GLASS_PANE);
        if (!sidedInv.canPlaceItemThroughFace(1, glassPane, Direction.UP)) {
            context.fail("Kiln should accept glass pane from top into glass input slot");
            return;
        }

        // Sand should also be accepted
        ItemStack sand = new ItemStack(Items.SAND);
        if (!sidedInv.canPlaceItemThroughFace(1, sand, Direction.UP)) {
            context.fail("Kiln should accept sand from top into glass input slot");
            return;
        }

        // Non-glass items should be rejected from top
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        if (sidedInv.canPlaceItemThroughFace(1, diamond, Direction.UP)) {
            context.fail("Kiln should NOT accept non-glass items from top");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln only accepts fuel from sides.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnFuelInputFromSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        // Coal should be accepted from sides into slot 0
        ItemStack coal = new ItemStack(Items.COAL);
        if (!sidedInv.canPlaceItemThroughFace(0, coal, Direction.NORTH)) {
            context.fail("Kiln should accept coal from sides into fuel slot");
            return;
        }

        // Test all horizontal sides
        for (Direction side : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (!sidedInv.canPlaceItemThroughFace(0, coal, side)) {
                context.fail("Kiln should accept fuel from " + side);
                return;
            }
        }

        context.succeed();
    }

    /**
     * Test that kiln only allows output extraction from bottom.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnOutputExtractionFromBottom(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        // Create test output item
        ItemStack testOutput = new ItemStack(Items.DIAMOND);

        // Bottom should allow extraction from output slot (11)
        if (!sidedInv.canTakeItemThroughFace(11, testOutput, Direction.DOWN)) {
            context.fail("Kiln should allow output extraction from bottom");
            return;
        }

        // Top and sides should NOT allow extraction
        if (sidedInv.canTakeItemThroughFace(11, testOutput, Direction.UP)) {
            context.fail("Kiln should NOT allow extraction from top");
            return;
        }

        if (sidedInv.canTakeItemThroughFace(11, testOutput, Direction.NORTH)) {
            context.fail("Kiln should NOT allow extraction from sides");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln crafting grid is NOT accessible from outside.
     * Slots 2-10 (crafting grid) should not be exposed via sided inventory.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnCraftingGridNotAccessible(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (!(kiln instanceof WorldlyContainer sidedInv)) {
            context.fail("Expected WorldlyContainer");
            return;
        }

        // Test that crafting grid slots (2-10) are not in any sided slot list
        for (Direction direction : Direction.values()) {
            int[] slots = sidedInv.getSlotsForFace(direction);
            for (int slot : slots) {
                if (slot >= 2 && slot <= 10) {
                    context.fail("Crafting grid slot " + slot + " should NOT be accessible from " + direction);
                    return;
                }
            }
        }

        context.succeed();
    }

    /**
     * Test that kiln has correct initial state.
     * Should start not burning and at zero temperature.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnInitialState(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        // Verify not burning initially
        if (kiln.isBurning()) {
            context.fail("Newly placed kiln should not be burning");
            return;
        }

        // Verify zero temperature initially
        if (kiln.getTemperature() != 0.0) {
            context.fail("Newly placed kiln should have zero temperature, got: " + kiln.getTemperature());
            return;
        }

        // Verify LIT state is false
        BlockState state = context.getBlockState(pos);
        if (state.getValue(KilnBlock.LIT)) {
            context.fail("Newly placed kiln should not be lit");
            return;
        }

        context.succeed();
    }

    /**
     * Test that kiln provides item storage capability.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnItemStorage(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        KilnBlockEntity kiln = (KilnBlockEntity) context.getBlockEntity(pos);

        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        // Verify kiln provides item storage from all sides
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
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnFacing(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        // Place kiln
        context.setBlock(pos, LogisticsCore.BLOCK.KILN);
        BlockState state = context.getBlockState(pos);

        // Verify FACING property exists
        if (!state.hasProperty(KilnBlock.FACING)) {
            context.fail("Kiln should have FACING property");
            return;
        }

        // Verify FACING is a valid horizontal direction
        Direction facing = state.getValue(KilnBlock.FACING);
        if (facing == Direction.UP || facing == Direction.DOWN) {
            context.fail("Kiln FACING should be horizontal, got: " + facing);
            return;
        }

        context.succeed();
    }
}
