package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the macerator GameTests. Test logic lives in
 * {@link MaceratorGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class MaceratorGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPlacementCreatesBlockEntity(GameTestHelper context) {
        MaceratorGameTestBody.testPlacementCreatesBlockEntity(context);
    }

    /**
    * Wiki claim (Usage): "Input is accepted from the top and sides; output is drawn from the
    * bottom."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testSidedAccess(GameTestHelper context) {
        MaceratorGameTestBody.testSidedAccess(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCapabilitiesExposed(GameTestHelper context) {
        MaceratorGameTestBody.testCapabilitiesExposed(context);
    }

    /**
    * Wiki claim (Usage): "...most ores take 2,000 RF (10 seconds)." (Recipes § Ores → Dust):
    * "Iron Ore -> Iron Dust,2".
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 240)
    public void testMaceratesOreWithEnergy(GameTestHelper context) {
        MaceratorGameTestBody.testMaceratesOreWithEnergy(context);
    }

    /**
    * Wiki claim (Usage/Power): "Input is accepted from the top and sides... connect a Stirling
    * Engine or any RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testMaceratesViaRealEngineAndHoppers(GameTestHelper context) {
        MaceratorGameTestBody.testMaceratesViaRealEngineAndHoppers(context);
    }
}
