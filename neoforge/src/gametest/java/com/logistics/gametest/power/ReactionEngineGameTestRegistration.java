package com.logistics.gametest.power;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link ReactionEngineGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class ReactionEngineGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("power/reaction_engine_placement", 100, ReactionEngineGameTestBody::testReactionEnginePlacement),
        new GameTestCase(
            "power/reaction_engine_exposes_no_energy_capability",
            100,
            ReactionEngineGameTestBody::testReactionEngineExposesNoEnergyCapability),
        new GameTestCase(
            "power/reaction_engine_reactant_filter", 100, ReactionEngineGameTestBody::testReactionEngineReactantFilter),
        new GameTestCase(
            "power/reaction_engine_catalyst_filter", 100, ReactionEngineGameTestBody::testReactionEngineCatalystFilter),
        new GameTestCase(
            "power/reaction_engine_ignites_and_delivers",
            60,
            ReactionEngineGameTestBody::testReactionEngineIgnitesAndDelivers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private ReactionEngineGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/reaction_engine", TESTS, FUNCTIONS);
    }
}
