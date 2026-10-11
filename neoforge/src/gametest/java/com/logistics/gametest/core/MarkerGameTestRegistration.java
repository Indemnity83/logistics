package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link MarkerGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class MarkerGameTestRegistration {

    private MarkerGameTestRegistration() {}

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testWrenchingCornerDefinesArea(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingCornerDefinesArea(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testWrenchingEdgeMarkerActivatesWholeSet(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingEdgeMarkerActivatesWholeSet(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testActivationFailsWithoutRightTriangle(GameTestHelper context) {
        MarkerGameTestBody.testActivationFailsWithoutRightTriangle(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testActivationFailsBelowMinimumSize(GameTestHelper context) {
        MarkerGameTestBody.testActivationFailsBelowMinimumSize(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testLoneMarkerActivatesWithoutArea(GameTestHelper context) {
        MarkerGameTestBody.testLoneMarkerActivatesWithoutArea(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testWrenchingActiveMarkerDeactivatesConnectedSet(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingActiveMarkerDeactivatesConnectedSet(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testBreakingActiveMarkerDeactivatesTheRest(GameTestHelper context) {
        MarkerGameTestBody.testBreakingActiveMarkerDeactivatesTheRest(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testMarkerBreaksInstantlyAndDropsItself(GameTestHelper context) {
        MarkerGameTestBody.testMarkerBreaksInstantlyAndDropsItself(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testMarkerHasNoCollisionAndStacksTo64(GameTestHelper context) {
        MarkerGameTestBody.testMarkerHasNoCollisionAndStacksTo64(context);
    }

    @GameTest(template = "empty", batch = "marker", timeoutTicks = 100)
    public static void testActiveMarkerSurvivesReconstruction(GameTestHelper context) {
        MarkerGameTestBody.testActiveMarkerSurvivesReconstruction(context);
    }
}
