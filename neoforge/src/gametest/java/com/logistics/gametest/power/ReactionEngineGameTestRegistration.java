package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link ReactionEngineGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class ReactionEngineGameTestRegistration {

    private ReactionEngineGameTestRegistration() {}

    @GameTest(template = "empty", batch = "reactionengine")
    public static void testReactionEnginePlacement(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEnginePlacement(context);
    }

    /** The bufferless guarantee: the engine exposes no energy capability on any face. */
    @GameTest(template = "empty", batch = "reactionengine")
    public static void testReactionEngineExposesNoEnergyCapability(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineExposesNoEnergyCapability(context);
    }

    /** The reactant tank accepts the launch reactant and rejects a non-reactant fluid. */
    @GameTest(template = "empty", batch = "reactionengine")
    public static void testReactionEngineReactantFilter(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineReactantFilter(context);
    }

    /** The catalyst slot accepts the launch catalyst, rejects other items, and is hidden on the output face. */
    @GameTest(template = "empty", batch = "reactionengine")
    public static void testReactionEngineCatalystFilter(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineCatalystFilter(context);
    }

    /**
    * A full reaction: with a reactant batch, a catalyst, and redstone, the engine ignites (consuming the
    * catalyst) and pushes energy into an adjacent battery.
    */
    @GameTest(template = "empty", batch = "reactionengine", timeoutTicks = 40)
    public static void testReactionEngineIgnitesAndDelivers(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineIgnitesAndDelivers(context);
    }
}
