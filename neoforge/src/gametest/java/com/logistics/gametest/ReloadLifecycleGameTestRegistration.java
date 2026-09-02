package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link ReloadLifecycleGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class ReloadLifecycleGameTestRegistration {

    private ReloadLifecycleGameTestRegistration() {}

    /**
    * Verifies a kiln smelt already in progress still completes after a datapack reload.
    *
    * <p>Run in-game: /test run logistics-gametest.reloadlifecyclegametest.kilncompletesinflightsmeltacrossreload
    */
    @GameTest(template = "empty", batch = "reloadlifecycle", timeoutTicks = 160)
    public static void kilnCompletesInFlightSmeltAcrossReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnCompletesInFlightSmeltAcrossReload(context);
    }

    /**
    * Verifies a kiln still starts a fresh smelt after a datapack reload.
    *
    * <p>Run in-game: /test run logistics-gametest.reloadlifecyclegametest.kilnstartsnewsmeltafterreload
    */
    @GameTest(template = "empty", batch = "reloadlifecycle", timeoutTicks = 170)
    public static void kilnStartsNewSmeltAfterReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnStartsNewSmeltAfterReload(context);
    }
}
