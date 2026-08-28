package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link SawmillGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class SawmillGameTestRegistration {

    private SawmillGameTestRegistration() {}

    @GameTest(template = "empty", batch = "sawmill")
    public static void testPlacementCreatesBlockEntity(GameTestHelper context) {
        SawmillGameTestBody.testPlacementCreatesBlockEntity(context);
    }

    /**
    * Wiki claim (Usage): "...input from the top and sides, primary and byproduct outputs drawn
    * from the bottom."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sawmill")
    public static void testSidedAccess(GameTestHelper context) {
        SawmillGameTestBody.testSidedAccess(context);
    }

    @GameTest(template = "empty", batch = "sawmill")
    public static void testCapabilitiesExposed(GameTestHelper context) {
        SawmillGameTestBody.testCapabilitiesExposed(context);
    }

    /**
    * Wiki claim (Usage): "...the Sawmill cuts it into planks over time and rolls its Sawdust
    * byproduct on completion." (Power): "Each recipe carries an RF cost (2,000–3,000 RF)."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sawmill", timeoutTicks = 200)
    public static void testSawsLogIntoPlanksAndByproduct(GameTestHelper context) {
        SawmillGameTestBody.testSawsLogIntoPlanksAndByproduct(context);
    }

    /** Verifies kelp and wheat seeds are both accepted as sawmill input at their required 8-item count. */
    @GameTest(template = "empty", batch = "sawmill")
    public static void testAcceptsKelpAndSeedsAsInput(GameTestHelper context) {
        SawmillGameTestBody.testAcceptsKelpAndSeedsAsInput(context);
    }

    /**
    * Pipes/hoppers probe insertion with a single-item template (see ContainerItemStorage), not a
    * stack already sized to the recipe's ingredientCount. A kelp recipe needing 8 must still accept
    * single-item deliveries so the slot can accumulate toward that count.
    */
    @GameTest(template = "empty", batch = "sawmill")
    public static void testAcceptsSingleKelpFromAPipeProbe(GameTestHelper context) {
        SawmillGameTestBody.testAcceptsSingleKelpFromAPipeProbe(context);
    }

    @GameTest(template = "empty", batch = "sawmill", timeoutTicks = 150)
    public static void testPulpsKelpIntoBiomass(GameTestHelper context) {
        SawmillGameTestBody.testPulpsKelpIntoBiomass(context);
    }

    @GameTest(template = "empty", batch = "sawmill", timeoutTicks = 150)
    public static void testPulpsSeedsIntoBiomass(GameTestHelper context) {
        SawmillGameTestBody.testPulpsSeedsIntoBiomass(context);
    }

    /**
    * Wiki claim (Usage/Power): "...input from the top and sides... connect a Stirling Engine or
    * any RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Usage">wiki/Sawmill.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sawmill", timeoutTicks = 240)
    public static void testSawsViaRealEngineAndHoppers(GameTestHelper context) {
        SawmillGameTestBody.testSawsViaRealEngineAndHoppers(context);
    }
}
