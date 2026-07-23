package com.logistics.gametest.power;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.fabric.storage.FabricItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Game tests for engines.
 * Tests energy production, fuel consumption, inventory access restrictions, and heat management.
 */
public class EngineGameTest {

    /**
     * Test that redstone engine can be placed and has block entity.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testRedstoneEnginePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsCore.BLOCK.REDSTONE_ENGINE);

        RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Redstone engine should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that stirling engine can be placed and has block entity.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testStirlingEnginePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE);

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that creative engine can be placed and has block entity.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCreativeEnginePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_ENGINE);

        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Creative engine should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that redstone engine has correct overheat behavior.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testRedstoneEngineCannotOverheat(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);

        // Place engine
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE);

        RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
        if (engine == null) {
            context.fail("Redstone engine should have block entity");
        }

        // Verify redstone engine cannot overheat
        if (engine.canOverheat()) {
            context.fail("Redstone engine should not be able to overheat");
        }

        context.succeed();
    }

    /**
     * Test that stirling engine's inventory is NOT accessible from the front face.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testStirlingEngineInventoryNotAccessibleFromFront(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place engine facing north
        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE
            .defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH));

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
        }

        // Try to access inventory from the front (NORTH) - should be null
        IItemStorage frontStorage = engine.itemStorage(Direction.NORTH);
        if (frontStorage != null) {
            context.fail("Stirling engine inventory should NOT be accessible from front face (NORTH)");
        }

        context.succeed();
    }

    /**
     * Test that stirling engine's inventory IS accessible from other sides.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testStirlingEngineInventoryAccessibleFromOtherSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place engine facing north
        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE
            .defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH));

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
        }

        // Try to access inventory from the back (SOUTH) - should work
        IItemStorage backStorage = engine.itemStorage(Direction.SOUTH);
        if (backStorage == null) {
            context.fail("Stirling engine inventory should be accessible from back face (SOUTH)");
        }

        // Try to access from other sides
        IItemStorage topStorage = engine.itemStorage(Direction.UP);
        if (topStorage == null) {
            context.fail("Stirling engine inventory should be accessible from top face");
        }

        IItemStorage sideStorage = engine.itemStorage(Direction.EAST);
        if (sideStorage == null) {
            context.fail("Stirling engine inventory should be accessible from side faces");
        }

        context.succeed();
    }

    /**
     * Test that stirling engine can accept fuel in its inventory.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testStirlingEngineAcceptsFuel(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place engine facing north
        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE
            .defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH));

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
        }

        // Insert coal from the back (valid fuel)
        IItemStorage backStorage = engine.itemStorage(Direction.SOUTH);
        if (backStorage == null) {
            context.fail("Back storage should be accessible");
        }

        long inserted = backStorage.insert(FabricItemKey.of(new ItemStack(Items.COAL)), 1, false);
        if (inserted != 1) {
            context.fail("Should be able to insert 1 coal, inserted: " + inserted);
        }

        // Verify coal was added to inventory
        ItemStack fuelStack = engine.getTheItem();
        if (!fuelStack.is(Items.COAL)) {
            context.fail("Engine inventory should contain coal, got: " + fuelStack);
        }

        context.succeed();
    }

    /**
     * Test that stirling engine rejects non-fuel items.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testStirlingEngineRejectsNonFuel(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place engine facing north
        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE
            .defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH));

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
        }

        // Try to insert dirt (not fuel) from the back
        IItemStorage backStorage = engine.itemStorage(Direction.SOUTH);
        if (backStorage == null) {
            context.fail("Back storage should be accessible");
        }

        long inserted = backStorage.insert(FabricItemKey.of(new ItemStack(Items.DIRT)), 1, false);
        if (inserted != 0) {
            context.fail("Should NOT be able to insert dirt (non-fuel), inserted: " + inserted);
        }

        // Verify inventory is still empty
        ItemStack fuelStack = engine.getTheItem();
        if (!fuelStack.isEmpty()) {
            context.fail("Engine inventory should be empty after rejecting dirt, got: " + fuelStack);
        }

        context.succeed();
    }

    /**
     * Test that creative engine cannot overheat.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCreativeEngineCannotOverheat(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_ENGINE);

        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Creative engine should have block entity");
        }

        // Creative engine should not be able to overheat
        if (engine.canOverheat()) {
            context.fail("Creative engine should not be able to overheat");
        }

        context.succeed();
    }

    /**
     * Test that creative engine has configurable output levels.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCreativeEngineOutputLevels(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_ENGINE);

        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Creative engine should have block entity");
        }

        // Default output level should be 20 RF/t (index 0)
        if (engine.getOutputRate() != 20L) {
            context.fail("Default output should be 20 RF/t, got: " + engine.getOutputRate());
        }

        // Cycle to next level
        long nextRate = engine.cycleOutputLevel();
        if (nextRate != 40L) {
            context.fail("Next output should be 40 RF/t, got: " + nextRate);
        }

        // Verify current rate matches
        if (engine.getOutputRate() != 40L) {
            context.fail("Current output should be 40 RF/t after cycle, got: " + engine.getOutputRate());
        }

        context.succeed();
    }

    /**
     * Test that creative sink can be configured with unlimited drain rate.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCreativeSinkUnlimitedDrain(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CreativeSinkBlockEntity sink = (CreativeSinkBlockEntity) context.getBlockEntity(pos);
        if (sink == null) {
            context.fail("Creative sink should have block entity");
        }

        // Default drain rate should be 5 RF/t
        if (sink.getDrainRate() != 5L) {
            context.fail("Default drain rate should be 5 RF/t, got: " + sink.getDrainRate());
        }

        // Set unlimited drain rate
        sink.setUnlimitedDrainRate();

        // Verify drain rate is now unlimited
        if (sink.getDrainRate() != Long.MAX_VALUE) {
            context.fail("Unlimited drain rate should be Long.MAX_VALUE, got: " + sink.getDrainRate());
        }

        context.succeed();
    }

    /**
     * Test that a powered redstone engine produces energy over time.
     *
     * <p>The redstone engine produces 10 RF every 16 game ticks when powered AND facing an
     * AcceptsLowTierEnergy block. After 20 ticks, at least one production interval should have
     * fired and the energy buffer should be non-zero.
     *
     * <p>Layout: [engine FACING=EAST POWERED=true] [creative sink]
     *
     * <p>Run in-game: /test run logistics-gametest.enginegametest.testredstoneengineproducesenergywhenpowered
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testRedstoneEngineProducesEnergyWhenPowered(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);
        BlockPos sinkPos = new BlockPos(1, 1, 0); // Engine output faces EAST toward the sink

        // Place sink first so isRunning() can detect it when the engine first ticks
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        // Redstone engine adds 10 RF every 16 game ticks.
        // After 20 ticks, at least one generation cycle should have fired.
        context.runAfterDelay(20, () -> {
            RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
            if (engine == null) {
                context.fail("Redstone engine block entity not found after 20 ticks");
                return;
            }
            if (engine.getEnergy() <= 0) {
                context.fail("Redstone engine should have energy after 20 ticks, stored: " + engine.getEnergy());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Test that a powered creative engine fills its energy buffer.
     *
     * <p>The creative engine sets its buffer to maximum capacity on every tick it is powered.
     * After just 5 ticks the buffer should be full.
     *
     * <p>Run in-game: /test run logistics-gametest.enginegametest.testcreativeengineaccumulatesenergywhenpowered
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCreativeEngineAccumulatesEnergyWhenPowered(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);

        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.POWERED, true));

        context.runAfterDelay(5, () -> {
            CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(enginePos);
            if (engine == null) {
                context.fail("Creative engine block entity not found after 5 ticks");
                return;
            }
            if (engine.getEnergy() != engine.getMaxEnergy()) {
                context.fail("Creative engine buffer should be full after 5 ticks, got: "
                        + engine.getEnergy() + " / " + engine.getMaxEnergy());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Test that a stirling engine burns fuel and accumulates energy in its buffer.
     *
     * <p>With coal in the fuel slot and POWERED=true, the engine should start burning
     * on the first tick, and after 100 ticks the buffer should be non-zero and the
     * engine should still be burning (coal burns for 1600 ticks).
     *
     * <p>Run in-game: /test run logistics-gametest.enginegametest.testStirlingEngineProducesEnergyFromFuel
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 120)
    public void testStirlingEngineProducesEnergyFromFuel(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        context.setBlock(pos, LogisticsPower.BLOCK.STIRLING_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.POWERED, true));

        StirlingEngineBlockEntity engine = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Stirling engine should have block entity");
            return;
        }

        engine.setTheItem(new ItemStack(Items.COAL));

        context.runAfterDelay(100, () -> {
            StirlingEngineBlockEntity eng = (StirlingEngineBlockEntity) context.getBlockEntity(pos);
            if (eng == null) {
                context.fail("Stirling engine block entity not found after 100 ticks");
                return;
            }
            if (eng.getEnergy() <= 0) {
                context.fail("Stirling engine should have energy after 100 ticks, got: " + eng.getEnergy());
                return;
            }
            if (eng.getBurnTime() <= 0) {
                context.fail("Stirling engine should still be burning (coal burns for 1600 ticks), got burnTime: "
                        + eng.getBurnTime());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Test that a redstone engine does NOT produce energy when unpowered.
     *
     * <p>An unpowered redstone engine decays existing energy and produces nothing new.
     * After 20 ticks with no redstone signal, the buffer should remain at zero.
     *
     * <p>Run in-game: /test run logistics-gametest.enginegametest.testredstoneengineproducesnoenergywhenunpowered
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testRedstoneEngineProducesNoEnergyWhenUnpowered(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);
        BlockPos sinkPos = new BlockPos(1, 1, 0);

        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        // Place engine with POWERED=false (default)
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, false));

        context.runAfterDelay(20, () -> {
            RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
            if (engine == null) {
                context.fail("Redstone engine block entity not found");
                return;
            }
            if (engine.getEnergy() != 0) {
                context.fail("Unpowered redstone engine should have zero energy, got: " + engine.getEnergy());
                return;
            }
            context.succeed();
        });
    }
}
