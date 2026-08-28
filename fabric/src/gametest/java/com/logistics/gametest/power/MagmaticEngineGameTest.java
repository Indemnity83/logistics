package com.logistics.gametest.power;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the magmatic engine GameTests. Test logic lives in
 * {@link MagmaticEngineGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class MagmaticEngineGameTest {

    /** The filtered fluid capability accepts lava (as a pipe would) and rejects everything else. */
    @GameTest
    public void testAcceptsLavaRejectsOther(GameTestHelper context) {
        MagmaticEngineGameTestBody.testAcceptsLavaRejectsOther(context);
    }

    /** Lava inserted through the capability feeds generation: the engine heat-soaks and delivers RF. */
    @GameTest(maxTicks = 120)
    public void testGeneratesFromLavaAndDelivers(GameTestHelper context) {
        MagmaticEngineGameTestBody.testGeneratesFromLavaAndDelivers(context);
    }
}
