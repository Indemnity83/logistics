package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.automation.sawmill.SawmillBlockEntity;
import com.logistics.core.lib.storage.IItemStorage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-world tests for the component-hosted Sawmill: capability routing, bottom-out sided access with
 * two output slots, and an end-to-end RF-cost run that produces the primary product plus the
 * chance-based byproduct.
 */
public class SawmillGameTest {

    private static final int INPUT = SawmillBlockEntity.INPUT_SLOT;
    private static final int PRIMARY = SawmillBlockEntity.PRIMARY_OUTPUT_SLOT;
    private static final int SECONDARY = SawmillBlockEntity.SECONDARY_OUTPUT_SLOT;

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPlacementCreatesBlockEntity(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if ((SawmillBlockEntity) context.getBlockEntity(pos) == null) {
            context.fail("Sawmill should create SawmillBlockEntity");
            return;
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testSidedAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (!(context.getBlockEntity(pos) instanceof WorldlyContainer sided)) {
            context.fail("Sawmill should implement WorldlyContainer");
            return;
        }

        int[] bottom = sided.getSlotsForFace(Direction.DOWN);
        if (bottom.length != 2 || bottom[0] != PRIMARY || bottom[1] != SECONDARY) {
            context.fail("Bottom should expose both output slots");
            return;
        }
        for (Direction side : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            int[] slots = sided.getSlotsForFace(side);
            if (slots.length != 1 || slots[0] != INPUT) {
                context.fail(side + " should expose the input slot");
                return;
            }
        }

        if (!sided.canTakeItemThroughFace(SECONDARY, ItemStack.EMPTY, Direction.DOWN)) {
            context.fail("Byproduct slot should be extractable from the bottom");
            return;
        }
        if (sided.canPlaceItemThroughFace(INPUT, new ItemStack(Items.OAK_LOG), Direction.DOWN)) {
            context.fail("Input should NOT be insertable from the bottom");
            return;
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCapabilitiesExposed(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = (SawmillBlockEntity) context.getBlockEntity(pos);

        if (sawmill.energyStorage(null) == null) {
            context.fail("Sawmill should expose energy storage");
            return;
        }
        for (Direction side : Direction.values()) {
            IItemStorage storage = sawmill.itemStorage(side);
            if (storage == null) {
                context.fail("Sawmill should expose item storage from " + side);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testSawsLogIntoPlanksAndByproduct(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = (SawmillBlockEntity) context.getBlockEntity(pos);

        var energy = sawmill.energyStorage(null);
        for (int i = 0; i < 200; i++) {
            energy.insert(128, false);
        }
        sawmill.setItem(INPUT, new ItemStack(Items.OAK_LOG));

        // Oak logs cost 3,000 RF and saw in 150 ticks at 20 RF/t -> 6 planks + sawdust byproduct.
        context.runAfterDelay(170, () -> {
            if (!sawmill.getItem(INPUT).isEmpty()) {
                context.fail("Input log should be consumed");
                return;
            }
            ItemStack primary = sawmill.getItem(PRIMARY);
            if (!primary.is(Items.OAK_PLANKS) || primary.getCount() != 6) {
                context.fail("Primary output should be 6 oak planks, got " + primary);
                return;
            }
            ItemStack byproduct = sawmill.getItem(SECONDARY);
            if (!byproduct.is(LogisticsCore.ITEM.SAWDUST) || byproduct.isEmpty()) {
                context.fail("Byproduct output should be sawdust, got " + byproduct);
                return;
            }
            context.succeed();
        });
    }

    /** Verifies kelp and wheat seeds are both accepted as sawmill input at their required 8-item count. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testAcceptsKelpAndSeedsAsInput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (!(context.getBlockEntity(pos) instanceof WorldlyContainer sided)) {
            context.fail("Sawmill should implement WorldlyContainer");
            return;
        }

        if (!sided.canPlaceItemThroughFace(INPUT, new ItemStack(Items.KELP, 8), Direction.UP)) {
            context.fail("Kelp should be insertable as a sawmill input");
            return;
        }
        if (!sided.canPlaceItemThroughFace(INPUT, new ItemStack(Items.WHEAT_SEEDS, 8), Direction.UP)) {
            context.fail("Wheat seeds should be insertable as a sawmill input");
            return;
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void testPulpsKelpIntoBiomass(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = (SawmillBlockEntity) context.getBlockEntity(pos);

        var energy = sawmill.energyStorage(null);
        for (int i = 0; i < 200; i++) {
            energy.insert(128, false);
        }
        sawmill.setItem(INPUT, new ItemStack(Items.KELP, 8));

        // Pulped-biomass recipes cost 2,000 RF and saw in 100 ticks at 20 RF/t -> 1 pulped biomass.
        context.runAfterDelay(120, () -> {
            if (!sawmill.getItem(INPUT).isEmpty()) {
                context.fail("Kelp input should be consumed, got " + sawmill.getItem(INPUT));
                return;
            }
            ItemStack primary = sawmill.getItem(PRIMARY);
            if (!primary.is(LogisticsCore.ITEM.PULPED_BIOMASS) || primary.getCount() != 1) {
                context.fail("Primary output should be 1 pulped biomass, got " + primary);
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void testPulpsSeedsIntoBiomass(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = (SawmillBlockEntity) context.getBlockEntity(pos);

        var energy = sawmill.energyStorage(null);
        for (int i = 0; i < 200; i++) {
            energy.insert(128, false);
        }
        sawmill.setItem(INPUT, new ItemStack(Items.WHEAT_SEEDS, 8));

        context.runAfterDelay(120, () -> {
            if (!sawmill.getItem(INPUT).isEmpty()) {
                context.fail("Seeds input should be consumed, got " + sawmill.getItem(INPUT));
                return;
            }
            ItemStack primary = sawmill.getItem(PRIMARY);
            if (!primary.is(LogisticsCore.ITEM.PULPED_BIOMASS) || primary.getCount() != 1) {
                context.fail("Primary output should be 1 pulped biomass, got " + primary);
                return;
            }
            context.succeed();
        });
    }
}
