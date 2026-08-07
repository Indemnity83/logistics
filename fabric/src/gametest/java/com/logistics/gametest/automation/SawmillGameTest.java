package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.automation.sawmill.SawmillBlockEntity;
import com.logistics.core.lib.storage.IItemStorage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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

    @GameTest
    public void testPlacementCreatesBlockEntity(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (context.getBlockEntity(pos, SawmillBlockEntity.class) == null) {
            context.fail("Sawmill should create SawmillBlockEntity");
            return;
        }
        context.succeed();
    }

    @GameTest
    public void testSidedAccess(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (!(context.getBlockEntity(pos, SawmillBlockEntity.class) instanceof WorldlyContainer sided)) {
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

    @GameTest
    public void testCapabilitiesExposed(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = context.getBlockEntity(pos, SawmillBlockEntity.class);

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

    @GameTest(maxTicks = 200)
    public void testSawsLogIntoPlanksAndByproduct(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = context.getBlockEntity(pos, SawmillBlockEntity.class);

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
    @GameTest
    public void testAcceptsKelpAndSeedsAsInput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (!(context.getBlockEntity(pos, SawmillBlockEntity.class) instanceof WorldlyContainer sided)) {
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

    /**
     * Pipes/hoppers probe insertion with a single-item template (see ContainerItemStorage), not a
     * stack already sized to the recipe's ingredientCount. A kelp recipe needing 8 must still accept
     * single-item deliveries so the slot can accumulate toward that count.
     */
    @GameTest
    public void testAcceptsSingleKelpFromAPipeProbe(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        if (!(context.getBlockEntity(pos, SawmillBlockEntity.class) instanceof WorldlyContainer sided)) {
            context.fail("Sawmill should implement WorldlyContainer");
            return;
        }

        if (!sided.canPlaceItemThroughFace(INPUT, new ItemStack(Items.KELP, 1), Direction.UP)) {
            context.fail("A single kelp item should be insertable even though the recipe needs 8");
            return;
        }
        context.succeed();
    }

    @GameTest(maxTicks = 150)
    public void testPulpsKelpIntoBiomass(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = context.getBlockEntity(pos, SawmillBlockEntity.class);

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

    @GameTest(maxTicks = 150)
    public void testPulpsSeedsIntoBiomass(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.SAWMILL);
        SawmillBlockEntity sawmill = context.getBlockEntity(pos, SawmillBlockEntity.class);

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
