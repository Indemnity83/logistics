package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link QuarryGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class QuarryGameTestRegistration {

    private QuarryGameTestRegistration() {}

    /**
    * Test that laser quarry can be placed and creates block entity.
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryPlacement(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPlacement(context);
    }

    /**
    * Test that laser quarry accepts energy from all sides.
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryAcceptsEnergy(GameTestHelper context) {
        QuarryGameTestBody.testQuarryAcceptsEnergy(context);
    }

    @GameTest(template = "empty", batch = "quarry", timeoutTicks = 20)
    public static void testQuarryTracksCommittedEnergyInput(GameTestHelper context) {
        QuarryGameTestBody.testQuarryTracksCommittedEnergyInput(context);
    }

    /**
    * Test that laser quarry does NOT accept items from pipes.
    * Quarry only outputs items, never accepts them.
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        QuarryGameTestBody.testQuarryDoesNotAcceptItems(context);
    }

    /**
    * Test that laser quarry starts in CLEARING phase.
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryInitialPhase(GameTestHelper context) {
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
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryHasNoCustomBoundsWithoutMarkers(GameTestHelper context) {
        QuarryGameTestBody.testQuarryHasNoCustomBoundsWithoutMarkers(context);
    }

    /**
    * Test that laser quarry reports correct pipe connection type.
    * Should only connect to pipes from above (Direction.UP).
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryPipeConnection(GameTestHelper context) {
        QuarryGameTestBody.testQuarryPipeConnection(context);
    }

    /**
    * Test that laser quarry block state has correct FACING property.
    */
    @GameTest(template = "empty", batch = "quarry")
    public static void testQuarryFacing(GameTestHelper context) {
        QuarryGameTestBody.testQuarryFacing(context);
    }
}
