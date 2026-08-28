package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the reaction engine GameTests. Test logic lives in
 * {@link ReactionEngineGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 *
 * <p>Tests the bufferless-output guarantee (no energy capability), the reactant/catalyst input
 * filters, and a full ignite-and-deliver cycle into an adjacent battery.
 */
public class ReactionEngineGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEnginePlacement(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEnginePlacement(context);
    }

    /** The bufferless guarantee: the engine exposes no energy capability on any face. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineExposesNoEnergyCapability(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineExposesNoEnergyCapability(context);
    }

    /** The reactant tank accepts the launch reactant and rejects a non-reactant fluid. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineReactantFilter(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineReactantFilter(context);
    }

    /** The catalyst slot accepts the launch catalyst, rejects other items, and is hidden on the output face. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineCatalystFilter(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineCatalystFilter(context);
    }

    /**
    * A full reaction: with a reactant batch, a catalyst, and redstone, the engine ignites (consuming the
    * catalyst) and pushes energy into an adjacent battery.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testReactionEngineIgnitesAndDelivers(GameTestHelper context) {
        ReactionEngineGameTestBody.testReactionEngineIgnitesAndDelivers(context);
    }
}
