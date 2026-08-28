package com.logistics.gametest.automation;

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
 * Wires {@link QuarryGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class QuarryGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/quarry_placement", 100, QuarryGameTestBody::testQuarryPlacement),
        new GameTestCase("automation/quarry_accepts_energy", 100, QuarryGameTestBody::testQuarryAcceptsEnergy),
        new GameTestCase(
            "automation/quarry_tracks_committed_energy_input", 40, QuarryGameTestBody::testQuarryTracksCommittedEnergyInput),
        new GameTestCase("automation/quarry_does_not_accept_items", 100, QuarryGameTestBody::testQuarryDoesNotAcceptItems),
        new GameTestCase("automation/quarry_initial_phase", 100, QuarryGameTestBody::testQuarryInitialPhase),
        new GameTestCase(
            "automation/quarry_has_no_custom_bounds_without_markers", 100, QuarryGameTestBody::testQuarryHasNoCustomBoundsWithoutMarkers),
        new GameTestCase("automation/quarry_pipe_connection", 100, QuarryGameTestBody::testQuarryPipeConnection),
        new GameTestCase("automation/quarry_facing", 100, QuarryGameTestBody::testQuarryFacing));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private QuarryGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/quarry", TESTS, FUNCTIONS);
    }
}
