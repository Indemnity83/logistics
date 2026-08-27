package com.logistics.gametest.core;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

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

    /**
     * Wiki claim (Usage): "Input is accepted from the top and sides; output is drawn from the
     * bottom."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
     */
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

    /**
     * Wiki claim (Usage): "...most ores take 2,000 RF (10 seconds)." (Recipes § Ores → Dust):
     * "Iron Ore -> Iron Dust,2".
     *
     * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
     */
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
        long filledEnergy = energy.getAmount();
        macerator.setItem(INPUT_SLOT, new ItemStack(Items.IRON_ORE));

        // iron_dust.json costs 2000 RF at 10 RF/t -> 200 ticks; 220 leaves setup slack.
        context.runAfterDelay(220, () -> {
            if (!macerator.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input ore should be consumed after maceration");
                return;
            }
            ItemStack output = macerator.getItem(OUTPUT_SLOT);
            if (output.isEmpty() || output.getCount() != 2 || !output.is(LogisticsCore.ITEM.IRON_DUST)) {
                context.fail("Macerator should have produced 2 iron dust, got: " + output);
                return;
            }
            long spent = filledEnergy - energy.getAmount();
            if (spent != 2_000) {
                context.fail("Grinding iron ore should cost exactly 2,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage/Power): "Input is accepted from the top and sides... connect a Stirling
     * Engine or any RF source."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
     */
    @GameTest(maxTicks = 260)
    public void testMaceratesViaRealEngineAndHoppers(GameTestHelper context) {
        BlockPos maceratorPos = new BlockPos(1, 1, 1);
        BlockPos enginePos = new BlockPos(0, 1, 1);
        BlockPos redstoneBlockPos = new BlockPos(-1, 1, 1);
        BlockPos inputHopperPos = maceratorPos.above();
        BlockPos outputHopperPos = maceratorPos.below();

        context.setBlock(maceratorPos, LogisticsAutomation.BLOCK.MACERATOR);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        context.setBlock(inputHopperPos, Blocks.HOPPER);
        context.setBlock(outputHopperPos, Blocks.HOPPER);

        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        HopperBlockEntity inputHopper = context.getBlockEntity(inputHopperPos, HopperBlockEntity.class);
        if (engine == null || inputHopper == null) {
            context.fail("Expected engine and input hopper block entities");
            return;
        }

        // Cycle 20 -> 40 -> 80 -> 160 RF/t, comfortably above the macerator's 128 RF/t input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();

        inputHopper.setItem(0, new ItemStack(Items.IRON_ORE));

        context.succeedWhen(() -> context.assertContainerContains(outputHopperPos, LogisticsCore.ITEM.IRON_DUST));
    }
}
