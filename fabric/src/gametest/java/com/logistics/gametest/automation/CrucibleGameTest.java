package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Crucible GameTests. Test logic lives in
 * {@link CrucibleGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class CrucibleGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPlacement(GameTestHelper context) {
        CrucibleGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
    * Pipes can drain the tank but never fill it directly — only the melting recipe does.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTankIsOutputOnly(GameTestHelper context) {
        CrucibleGameTestBody.testTankIsOutputOnly(context);
    }

    /**
    * Wiki claim (Recipes § Lava & water): "Ice -> Water, Amount=1000."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Lava_.26_water">wiki/Crucible.txt § Lava & water</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testMeltsIceIntoWater(GameTestHelper context) {
        CrucibleGameTestBody.testMeltsIceIntoWater(context);
    }

    /**
    * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testMeltsViaRealEngineAndHopper(GameTestHelper context) {
        CrucibleGameTestBody.testMeltsViaRealEngineAndHopper(context);
    }
}
