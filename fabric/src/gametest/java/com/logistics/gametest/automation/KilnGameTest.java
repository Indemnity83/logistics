package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.core.lib.storage.IItemStorage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
     * Wiki claim (Usage): "Input is accepted from the top and sides; output is drawn from the
     * bottom, the same as a vanilla furnace."
     *
     * <p>NOTE: the wiki overstates this — the code (and this test) enforce input from the top only,
     * matching vanilla furnace side-access rules. See WIKI_DISCREPANCIES.md § Kiln.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
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
     * Wiki claim (Usage): "Input is accepted from the top and sides..."
     *
     * <p>NOTE: the wiki overstates this — the code (and this test) enforce input from the top only.
     * See WIKI_DISCREPANCIES.md § Kiln.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
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
     * Wiki claim (Usage): "...output is drawn from the bottom, the same as a vanilla furnace."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
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
            IItemStorage storage = kiln.itemStorage(direction);
            if (storage == null) {
                context.fail("Kiln should provide item storage from " + direction);
                return;
            }
        }

        context.succeed();
    }

    /**
     * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/tick. Each smelt draws RF
     * over the same time a vanilla furnace would take — about 4,000 RF for a standard 10-second
     * smelt (20 RF/tick while active)."
     *
     * <p>NOTE: the code computes cookingTime(200) * KILN_RF_PER_COOK_TICK(10) = 2,000 RF, drained at
     * 20 RF/t = 100 ticks (5s) — half the wiki's claimed cost and time. This test asserts the code's
     * actual behavior; see WIKI_DISCREPANCIES.md § Kiln for the tracked mismatch.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Power">wiki/Kiln.txt § Power</a>
     */
    @GameTest(maxTicks = 130)
    public void testKilnSmeltsWithEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);
        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        var energy = kiln.energyStorage(null);

        // Insert-rate cap: a single over-sized insert is clamped to 128 RF/t, and the cap applies
        // per call, not once — a second identical call brings the total to 256.
        long firstInsert = energy.insert(500, false);
        if (firstInsert != 128) {
            context.fail("Kiln should accept at most 128 RF per insert, got: " + firstInsert);
            return;
        }
        long secondInsert = energy.insert(500, false);
        if (secondInsert != 128 || energy.getAmount() != 256) {
            context.fail("Kiln's 128 RF/t cap should apply per call, got amount: " + energy.getAmount());
            return;
        }

        // Capacity cap: fill to exactly 10,000 RF, then confirm a further insert is rejected.
        for (int i = 0; i < 78; i++) {
            energy.insert(128, false);
        }
        if (energy.getAmount() != 10_000) {
            context.fail("Kiln should hold exactly 10,000 RF, got: " + energy.getAmount());
            return;
        }
        if (energy.insert(500, false) != 0) {
            context.fail("A full Kiln should reject further energy");
            return;
        }

        kiln.setItem(0, new ItemStack(Items.RAW_IRON));

        // Mid-smelt: the kiln must already be drawing energy, so a stalled/no-tick regression fails early.
        context.runAfterDelay(60, () -> {
            if (energy.getAmount() >= 10_000) {
                context.fail("Kiln should be consuming energy while smelting");
            }
        });

        // Raw iron (200-tick recipe) costs cookingTime(200) * KILN_RF_PER_COOK_TICK(10) = 2,000 RF,
        // spent at KILN_ENERGY_PER_TICK(20) RF/t = 100 ticks. A tight window here also guards against
        // a smelt-speed regression, not just an eventually-true completion.
        context.runAfterDelay(110, () -> {
            if (!kiln.getItem(0).isEmpty()) {
                context.fail("Input should be consumed after smelting");
                return;
            }
            ItemStack output = kiln.getItem(1);
            if (!output.is(Items.IRON_INGOT)) {
                context.fail("Kiln should have smelted raw iron into an iron ingot");
                return;
            }
            long spent = 10_000 - energy.getAmount();
            if (spent != 2_000) {
                context.fail("A standard smelt should cost exactly 2,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Setup): "The Kiln smelts continuously as long as it has power and input items."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Setup">wiki/Kiln.txt § Setup</a>
     */
    @GameTest(maxTicks = 230)
    public void testKilnSmeltsContinuously(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(pos, KilnBlockEntity.class);
        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return;
        }

        // Fill the energy buffer (enough for several smelts) and queue two raw iron up front.
        var energy = kiln.energyStorage(null);
        for (int i = 0; i < 79; i++) {
            energy.insert(128, false);
        }
        kiln.setItem(0, new ItemStack(Items.RAW_IRON, 2));

        long[] energyAtFirstCompletion = new long[1];

        // First item completes at ~100 ticks (see testKilnSmeltsWithEnergy); confirm one was
        // consumed, one remains queued, and record energy at this point.
        context.runAfterDelay(110, () -> {
            if (kiln.getItem(0).getCount() != 1) {
                context.fail("One raw iron should remain queued after the first smelt, got: "
                    + kiln.getItem(0).getCount());
                return;
            }
            if (kiln.getItem(1).getCount() < 1) {
                context.fail("First smelt should have produced an iron ingot");
                return;
            }
            energyAtFirstCompletion[0] = energy.getAmount();
        });

        // A few ticks later, the second item should already be drawing energy — no idle gap
        // between recipes while power and input remain available.
        context.runAfterDelay(115, () -> {
            if (energy.getAmount() >= energyAtFirstCompletion[0]) {
                context.fail("Kiln should resume smelting immediately after the first item completes");
            }
        });

        // Second item completes ~100 ticks after the first (by ~210 total); confirm both are done.
        context.runAfterDelay(220, () -> {
            if (!kiln.getItem(0).isEmpty()) {
                context.fail("Both raw iron should be consumed after two smelts");
                return;
            }
            if (kiln.getItem(1).getCount() != 2) {
                context.fail("Kiln should have smelted both raw iron into iron ingots, got count: "
                    + kiln.getItem(1).getCount());
                return;
            }
            context.succeed();
        });
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
