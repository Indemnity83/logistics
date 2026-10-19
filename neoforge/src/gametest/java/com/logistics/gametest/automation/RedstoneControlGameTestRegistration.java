package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link RedstoneControlGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class RedstoneControlGameTestRegistration {

    private RedstoneControlGameTestRegistration() {}

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 160)
    public static void testIgnoredRunsWithAndWithoutSignal(GameTestHelper context) {
        RedstoneControlGameTestBody.testIgnoredRunsWithAndWithoutSignal(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 200)
    public static void testHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testHighPausesUntilSignalled(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 200)
    public static void testLowPausesWhileSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testLowPausesWhileSignalled(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 220)
    public static void testModeChangeMidRunFreezesProgress(GameTestHelper context) {
        RedstoneControlGameTestBody.testModeChangeMidRunFreezesProgress(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 260)
    public static void testLeverTogglesPauseAndResume(GameTestHelper context) {
        RedstoneControlGameTestBody.testLeverTogglesPauseAndResume(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 240)
    public static void testFabricatorHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testFabricatorHighPausesUntilSignalled(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol", timeoutTicks = 140)
    public static void testTransposerHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testTransposerHighPausesUntilSignalled(context);
    }

    @GameTest(template = "empty", batch = "redstonecontrol")
    public static void testModePersistsAndDefaultIsNotSaved(GameTestHelper context) {
        RedstoneControlGameTestBody.testModePersistsAndDefaultIsNotSaved(context);
    }
}
