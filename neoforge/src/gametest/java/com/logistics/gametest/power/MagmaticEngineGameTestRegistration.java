package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link MagmaticEngineGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class MagmaticEngineGameTestRegistration {

    private MagmaticEngineGameTestRegistration() {}

    /** The filtered fluid capability accepts lava (as a pipe would) and rejects everything else. */
    @GameTest(template = "empty", batch = "magmaticengine")
    public static void testAcceptsLavaRejectsOther(GameTestHelper context) {
        MagmaticEngineGameTestBody.testAcceptsLavaRejectsOther(context);
    }

    /** Lava inserted through the capability feeds generation: the engine heat-soaks and delivers RF. */
    @GameTest(template = "empty", batch = "magmaticengine", timeoutTicks = 120)
    public static void testGeneratesFromLavaAndDelivers(GameTestHelper context) {
        MagmaticEngineGameTestBody.testGeneratesFromLavaAndDelivers(context);
    }
}
