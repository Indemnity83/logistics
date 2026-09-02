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

    /**
    * Verifies a placed cable exposes an energy storage.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcableplacementexposesenergystorage
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCablePlacementExposesEnergyStorage(GameTestHelper context) {
        CableGameTestBody.testCablePlacementExposesEnergyStorage(context);
    }

    /**
    * Verifies energy inserted into a cable passes through to the machine behind it.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testinsertedcableenergypassesthroughtomachine
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testInsertedCableEnergyPassesThroughToMachine(GameTestHelper context) {
        CableGameTestBody.testInsertedCableEnergyPassesThroughToMachine(context);
    }

    /**
    * Verifies a cable charges an idle machine's buffer.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcablechargesidlemachinebuffer
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCableChargesIdleMachineBuffer(GameTestHelper context) {
        CableGameTestBody.testCableChargesIdleMachineBuffer(context);
    }

    /**
    * Verifies cable insertion splits energy according to each machine's reported demand.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcableinsertiondistributesbyreporteddemand
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testCableInsertionDistributesByReportedDemand(GameTestHelper context) {
        CableGameTestBody.testCableInsertionDistributesByReportedDemand(context);
    }

    /**
    * Verifies a mixed-tier cable route is capped by its weakest cable.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testmixedtierrouteiscappedbyweakestcable
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void testMixedTierRouteIsCappedByWeakestCable(GameTestHelper context) {
        CableGameTestBody.testMixedTierRouteIsCappedByWeakestCable(context);
    }

    /**
    * Verifies a cable network stops delivering past a cable that was removed.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcablenetworkstopsatremovedcable
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 60)
    public static void testCableNetworkStopsAtRemovedCable(GameTestHelper context) {
        CableGameTestBody.testCableNetworkStopsAtRemovedCable(context);
    }

    /**
    * Verifies a cable network rejoins once a removed cable is restored.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.testcablenetworkrejoinsaftercableisrestored
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 60)
    public static void testCableNetworkRejoinsAfterCableIsRestored(GameTestHelper context) {
        CableGameTestBody.testCableNetworkRejoinsAfterCableIsRestored(context);
    }

    /**
    * Verifies a cable does not power an extraction pipe.
    *
    * <p>The body doesn't call succeed() itself — Fabric's wrapper adds a grid-visibility assertion
    * afterward (see {@link CableGameTestBody}), so NeoForge finishes it here instead.
    *
    * <p>Run in-game: /test run logistics-gametest.cablegametest.cabledoesnotpowerextractionpipe
    */
    @GameTest(template = "empty", batch = "cable", timeoutTicks = 100)
    public static void cableDoesNotPowerExtractionPipe(GameTestHelper context) {
        if (CableGameTestBody.cableDoesNotPowerExtractionPipe(context) != null) {
            context.succeed();
        }
    }
}
