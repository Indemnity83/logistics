package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the laser quarry GameTests. Test logic lives in
 * {@link QuarryGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class QuarryGameTest {

    /**
    * Test that laser quarry can be placed and creates block entity.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testQuarryPlacement(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPlacement(context);
    }

    /**
     * A player can wire the quarry from any face — proven with a real creative engine and copper
     * cable on each of the six faces in turn, not by asking the block entity for an energy storage.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void testQuarryAcceptsEnergy(GameTestHelper context) {
        QuarryGameTestBody.testQuarryAcceptsEnergy(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 20)
    public void testQuarryTracksCommittedEnergyInput(GameTestHelper context) {
        QuarryGameTestBody.testQuarryTracksCommittedEnergyInput(context);
    }

    /**
     * The quarry only pushes items out — real hoppers aimed at every reachable face must keep
     * hold of their items, and nothing may end up on the ground.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        QuarryGameTestBody.testQuarryDoesNotAcceptItems(context);
    }

    /**
    * Test that laser quarry starts in CLEARING phase.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testQuarryInitialPhase(GameTestHelper context) {
        QuarryGameTestBody.testQuarryInitialPhase(context);
    }

    /**
    * Wiki claim (Mining area): "Default (no markers): mines a 16×16 area centered on the quarry's
    * placement." Placing a quarry with no adjacent markers (via the real {@code setPlacedBy} path,
    * not just a raw block-state write) leaves custom bounds unset, so it falls back to that default
    * (the 16 config value itself is asserted in {@code common/src/test/.../laserquarry/LaserQuarryConfigTest}
    * — this test doesn't measure the resulting area).
    *
    * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Mining_area">wiki/Laser Quarry.txt § Mining area</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testQuarryHasNoCustomBoundsWithoutMarkers(GameTestHelper context) {
        QuarryGameTestBody.testQuarryHasNoCustomBoundsWithoutMarkers(context);
    }

    /**
     * Only the quarry's top face takes a pipe — read from real pipes on all six faces, after the
     * server has ticked their connection caches.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testQuarryPipeConnection(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPipeConnection(context);
    }

    /**
    * Test that laser quarry block state has correct FACING property.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testQuarryFacing(GameTestHelper context) {
        QuarryGameTestBody.testQuarryFacing(context);
    }
}
