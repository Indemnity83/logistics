package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Marker GameTests. Test logic lives in
 * {@link MarkerGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class MarkerGameTest {

    /**
     * Wiki claim (Defining an area): "Place three markers in a right-triangle (L-shape)... Activate
     * the markers with the Wrench" and "A successful activation reports the defined area size."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testWrenchingCornerDefinesArea(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingCornerDefinesArea(context);
    }

    /**
     * Wiki claim (Defining an area): "Activate the markers with the Wrench" — any marker of the L.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testWrenchingEdgeMarkerActivatesWholeSet(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingEdgeMarkerActivatesWholeSet(context);
    }

    /**
     * Wiki claim (Defining an area): "Activation fails if there are not three markers in a right
     * triangle."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testActivationFailsWithoutRightTriangle(GameTestHelper context) {
        MarkerGameTestBody.testActivationFailsWithoutRightTriangle(context);
    }

    /**
     * Wiki claim (Defining an area): "Activation fails ... if the resulting area is below the
     * minimum size."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testActivationFailsBelowMinimumSize(GameTestHelper context) {
        MarkerGameTestBody.testActivationFailsBelowMinimumSize(context);
    }

    /**
     * Wiki claim (Defining an area): "Activation fails if there are not three markers in a right
     * triangle."
     *
     * <p>NOTE: a lone marker activates on its own instead. See WIKI_DISCREPANCIES.md § Marker.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testLoneMarkerActivatesWithoutArea(GameTestHelper context) {
        MarkerGameTestBody.testLoneMarkerActivatesWithoutArea(context);
    }

    /**
     * Wiki claim (Defining an area): "Wrenching an active marker again deactivates the connected
     * set."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testWrenchingActiveMarkerDeactivatesConnectedSet(GameTestHelper context) {
        MarkerGameTestBody.testWrenchingActiveMarkerDeactivatesConnectedSet(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testBreakingActiveMarkerDeactivatesTheRest(GameTestHelper context) {
        MarkerGameTestBody.testBreakingActiveMarkerDeactivatesTheRest(context);
    }

    /**
     * Wiki claim (Defining an area): "Markers ... break instantly with any tool, dropping
     * themselves." Infobox: hardness 0, blast resistance 0.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMarkerBreaksInstantlyAndDropsItself(GameTestHelper context) {
        MarkerGameTestBody.testMarkerBreaksInstantlyAndDropsItself(context);
    }

    /**
     * Wiki claim (Defining an area): "Markers have no collision — you can walk through them."
     * Infobox: stackable to 64.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMarkerHasNoCollisionAndStacksTo64(GameTestHelper context) {
        MarkerGameTestBody.testMarkerHasNoCollisionAndStacksTo64(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testActiveMarkerSurvivesReconstruction(GameTestHelper context) {
        MarkerGameTestBody.testActiveMarkerSurvivesReconstruction(context);
    }
}
