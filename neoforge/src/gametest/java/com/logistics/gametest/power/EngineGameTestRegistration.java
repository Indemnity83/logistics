package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link EngineGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class EngineGameTestRegistration {

    private EngineGameTestRegistration() {}

    /**
    * Test that redstone engine can be placed and has block entity.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testRedstoneEnginePlacement(GameTestHelper context) {
        EngineGameTestBody.testRedstoneEnginePlacement(context);
    }

    /**
    * Test that stirling engine can be placed and has block entity.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testStirlingEnginePlacement(GameTestHelper context) {
        EngineGameTestBody.testStirlingEnginePlacement(context);
    }

    /**
    * Test that creative engine can be placed and has block entity.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testCreativeEnginePlacement(GameTestHelper context) {
        EngineGameTestBody.testCreativeEnginePlacement(context);
    }

    /**
    * Test that redstone engine has correct overheat behavior.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testRedstoneEngineCannotOverheat(GameTestHelper context) {
        EngineGameTestBody.testRedstoneEngineCannotOverheat(context);
    }

    /**
    * Test that stirling engine's inventory is NOT accessible from the front face.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testStirlingEngineInventoryNotAccessibleFromFront(GameTestHelper context) {
        EngineGameTestBody.testStirlingEngineInventoryNotAccessibleFromFront(context);
    }

    /**
    * Test that stirling engine's inventory IS accessible from other sides.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testStirlingEngineInventoryAccessibleFromOtherSides(GameTestHelper context) {
        EngineGameTestBody.testStirlingEngineInventoryAccessibleFromOtherSides(context);
    }

    /**
    * Test that stirling engine can accept fuel in its inventory.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testStirlingEngineAcceptsFuel(GameTestHelper context) {
        EngineGameTestBody.testStirlingEngineAcceptsFuel(context);
    }

    /**
    * Test that stirling engine rejects non-fuel items.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testStirlingEngineRejectsNonFuel(GameTestHelper context) {
        EngineGameTestBody.testStirlingEngineRejectsNonFuel(context);
    }

    /**
    * Test that creative engine cannot overheat.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testCreativeEngineCannotOverheat(GameTestHelper context) {
        EngineGameTestBody.testCreativeEngineCannotOverheat(context);
    }

    /**
    * Test that creative engine has configurable output levels.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testCreativeEngineOutputLevels(GameTestHelper context) {
        EngineGameTestBody.testCreativeEngineOutputLevels(context);
    }

    /**
    * Test that creative sink can be configured with unlimited drain rate.
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testCreativeSinkUnlimitedDrain(GameTestHelper context) {
        EngineGameTestBody.testCreativeSinkUnlimitedDrain(context);
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
    @GameTest(template = "empty", batch = "engine", timeoutTicks = 30)
    public static void testRedstoneEngineProducesEnergyWhenPowered(GameTestHelper context) {
        EngineGameTestBody.testRedstoneEngineProducesEnergyWhenPowered(context);
    }

    /**
    * Test that a powered creative engine fills its energy buffer.
    *
    * <p>The creative engine sets its buffer to maximum capacity on every tick it is powered.
    * After just 5 ticks the buffer should be full.
    *
    * <p>Run in-game: /test run logistics-gametest.enginegametest.testcreativeengineaccumulatesenergywhenpowered
    */
    @GameTest(template = "empty", batch = "engine")
    public static void testCreativeEngineAccumulatesEnergyWhenPowered(GameTestHelper context) {
        EngineGameTestBody.testCreativeEngineAccumulatesEnergyWhenPowered(context);
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
    @GameTest(template = "empty", batch = "engine", timeoutTicks = 120)
    public static void testStirlingEngineProducesEnergyFromFuel(GameTestHelper context) {
        EngineGameTestBody.testStirlingEngineProducesEnergyFromFuel(context);
    }

    /**
    * Test that a redstone engine does NOT produce energy when unpowered.
    *
    * <p>An unpowered redstone engine decays existing energy and produces nothing new.
    * After 20 ticks with no redstone signal, the buffer should remain at zero.
    *
    * <p>Run in-game: /test run logistics-gametest.enginegametest.testredstoneengineproducesnoenergywhenunpowered
    */
    @GameTest(template = "empty", batch = "engine", timeoutTicks = 30)
    public static void testRedstoneEngineProducesNoEnergyWhenUnpowered(GameTestHelper context) {
        EngineGameTestBody.testRedstoneEngineProducesNoEnergyWhenUnpowered(context);
    }
}
