package com.logistics.gametest.core;

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
 * Wires {@link MarkerGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class MarkerGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("core/marker_wrenching_corner_defines_area", 100, MarkerGameTestBody::testWrenchingCornerDefinesArea),
        new GameTestCase(
            "core/marker_wrenching_edge_activates_whole_set",
            100,
            MarkerGameTestBody::testWrenchingEdgeMarkerActivatesWholeSet),
        new GameTestCase(
            "core/marker_activation_fails_without_right_triangle",
            100,
            MarkerGameTestBody::testActivationFailsWithoutRightTriangle),
        new GameTestCase(
            "core/marker_activation_fails_below_minimum_size",
            100,
            MarkerGameTestBody::testActivationFailsBelowMinimumSize),
        new GameTestCase(
            "core/marker_lone_marker_activates_without_area", 100, MarkerGameTestBody::testLoneMarkerActivatesWithoutArea),
        new GameTestCase(
            "core/marker_wrenching_active_marker_deactivates_set",
            100,
            MarkerGameTestBody::testWrenchingActiveMarkerDeactivatesConnectedSet),
        new GameTestCase(
            "core/marker_breaking_active_marker_deactivates_rest",
            100,
            MarkerGameTestBody::testBreakingActiveMarkerDeactivatesTheRest),
        new GameTestCase(
            "core/marker_breaks_instantly_and_drops_itself",
            100,
            MarkerGameTestBody::testMarkerBreaksInstantlyAndDropsItself),
        new GameTestCase(
            "core/marker_no_collision_and_stacks_to_64", 100, MarkerGameTestBody::testMarkerHasNoCollisionAndStacksTo64),
        new GameTestCase(
            "core/marker_active_marker_survives_reconstruction",
            100,
            MarkerGameTestBody::testActiveMarkerSurvivesReconstruction),
        new GameTestCase(
            "core/marker_pops_off_when_floor_removed", 100, MarkerGameTestBody::testMarkerPopsOffWhenFloorRemoved),
        new GameTestCase(
            "core/marker_removing_active_marker_without_player_deactivates_rest",
            100,
            MarkerGameTestBody::testRemovingActiveMarkerWithoutPlayerDeactivatesTheRest),
        new GameTestCase(
            "core/marker_active_marker_popping_off_deactivates_rest",
            100,
            MarkerGameTestBody::testActiveMarkerPoppingOffDeactivatesTheRest));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private MarkerGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "core/marker", TESTS, FUNCTIONS);
    }
}
