package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CableGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CableGameTestRegistration {

    private CableGameTestRegistration() {}

    /**
    * Verifies a creative engine powers a machine through a cable network.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcreativeenginepowerscablenetwork
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 60)
    public static void testCreativeEnginePowersCableNetwork(GameTestHelper context) {
        CableGameTestBody.testCreativeEnginePowersCableNetwork(context);
    }

    /**
    * Verifies a cable's cached connection updates when a neighbouring engine's output rotates.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcableconnectionupdateswhenneighboroutputrotates
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCableConnectionUpdatesWhenNeighborOutputRotates(GameTestHelper context) {
        CableGameTestBody.testCableConnectionUpdatesWhenNeighborOutputRotates(context);
    }

    /**
    * Verifies a cable connects to engine outputs that have no internal buffer.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcableconnectstobufferlessengineoutputs
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCableConnectsToBufferlessEngineOutputs(GameTestHelper context) {
        CableGameTestBody.testCableConnectsToBufferlessEngineOutputs(context);
    }

    /**
    * Verifies a cable does not connect to a redstone engine.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcabledoesnotconnecttoredstoneengine
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCableDoesNotConnectToRedstoneEngine(GameTestHelper context) {
        CableGameTestBody.testCableDoesNotConnectToRedstoneEngine(context);
    }

    /**
    * Verifies a cable network does not pull energy out of a redstone engine.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testredstoneengineisnotpulledbycablenetwork
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 90)
    public static void testRedstoneEngineIsNotPulledByCableNetwork(GameTestHelper context) {
        CableGameTestBody.testRedstoneEngineIsNotPulledByCableNetwork(context);
    }
}
