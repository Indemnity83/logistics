package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Sawmill GameTests. Test logic lives in
 * {@link SawmillGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class SawmillGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPlacementCreatesBlockEntity(GameTestHelper context) {
        SawmillGameTestBody.testPlacementCreatesBlockEntity(context);
    }

    /**
    * Wiki claim (Usage): "...input from the top and sides, primary and byproduct outputs drawn
    * from the bottom."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testSidedAccess(GameTestHelper context) {
        SawmillGameTestBody.testSidedAccess(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCapabilitiesExposed(GameTestHelper context) {
        SawmillGameTestBody.testCapabilitiesExposed(context);
    }

    /**
    * Wiki claim (Usage): "...the Sawmill cuts it into planks over time and rolls its Sawdust
    * byproduct on completion." (Power): "Each recipe carries an RF cost (2,000–3,000 RF)."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testSawsLogIntoPlanksAndByproduct(GameTestHelper context) {
        SawmillGameTestBody.testSawsLogIntoPlanksAndByproduct(context);
    }

    /** Verifies kelp and wheat seeds are both accepted as sawmill input at their required 8-item count. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testAcceptsKelpAndSeedsAsInput(GameTestHelper context) {
        SawmillGameTestBody.testAcceptsKelpAndSeedsAsInput(context);
    }

    /**
    * Pipes/hoppers probe insertion with a single-item template (see ContainerItemStorage), not a
    * stack already sized to the recipe's ingredientCount. A kelp recipe needing 8 must still accept
    * single-item deliveries so the slot can accumulate toward that count.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testAcceptsSingleKelpFromAPipeProbe(GameTestHelper context) {
        SawmillGameTestBody.testAcceptsSingleKelpFromAPipeProbe(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void testPulpsKelpIntoBiomass(GameTestHelper context) {
        SawmillGameTestBody.testPulpsKelpIntoBiomass(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void testPulpsSeedsIntoBiomass(GameTestHelper context) {
        SawmillGameTestBody.testPulpsSeedsIntoBiomass(context);
    }

    /**
    * Wiki claim (Usage/Power): "...input from the top and sides... connect a Stirling Engine or
    * any RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 240)
    public void testSawsViaRealEngineAndHoppers(GameTestHelper context) {
        SawmillGameTestBody.testSawsViaRealEngineAndHoppers(context);
    }
}
