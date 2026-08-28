package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
    @GameTest
    public void testQuarryPlacement(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPlacement(context);
    }

    /**
     * Test that laser quarry accepts energy from all sides.
     */
    @GameTest
    public void testQuarryAcceptsEnergy(GameTestHelper context) {
        QuarryGameTestBody.testQuarryAcceptsEnergy(context);
    }

    @GameTest(maxTicks = 20)
    public void testQuarryTracksCommittedEnergyInput(GameTestHelper context) {
        QuarryGameTestBody.testQuarryTracksCommittedEnergyInput(context);
    }

    /**
     * Test that laser quarry does NOT accept items from pipes.
     * Quarry only outputs items, never accepts them.
     */
    @GameTest
    public void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        QuarryGameTestBody.testQuarryDoesNotAcceptItems(context);
    }

    /**
     * Test that laser quarry starts in CLEARING phase.
     */
    @GameTest
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
    @GameTest
    public void testQuarryHasNoCustomBoundsWithoutMarkers(GameTestHelper context) {
        QuarryGameTestBody.testQuarryHasNoCustomBoundsWithoutMarkers(context);
    }

    /**
     * Test that laser quarry reports correct pipe connection type.
     * Should only connect to pipes from above (Direction.UP).
     */
    @GameTest
    public void testQuarryPipeConnection(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPipeConnection(context);
    }

    /**
     * Test that laser quarry block state has correct FACING property.
     */
    @GameTest
    public void testQuarryFacing(GameTestHelper context) {
        QuarryGameTestBody.testQuarryFacing(context);
    }
}
