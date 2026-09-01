package com.logistics.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Proves the client feature test pipeline works end to end: a real client starts with the mod
 * loaded, builds a world, renders it, and captures a screenshot. Deliberately asserts nothing about
 * our own blocks yet — that is what the showcase tests will do. Its job is to fail loudly if the
 * client test harness itself breaks, which is otherwise only discovered when someone tries to add
 * a real client test.
 *
 * <p>Unlike the server-side feature tests, this is not vanilla GameTest and carries no
 * {@code @GameTest} methods: the Fabric client test framework runs a whole class as one script via
 * the {@code fabric-client-gametest} entrypoint. It is therefore invisible to
 * {@code checkFeatureTestParity}, which scans {@code @GameTest} methods, and has no NeoForge
 * counterpart — see TESTING.md.
 *
 * <p>The determinism steps below are load-bearing; see "Client feature tests" in TESTING.md for why
 * each one matters.
 */
public class ClientBootstrapGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        // Resets GUI scale, render distance, and graphics options, so a developer's local settings
        // can't change what gets captured.
        context.restoreDefaultGameOptions();

        try (TestSingleplayerContext singleplayer =
                context.worldBuilder().setUseConsistentSettings(true).create()) {
            // Capturing before chunks finish rendering yields a partly-empty frame.
            singleplayer.getClientLevel().waitForChunksRender();

            // disableCounterPrefix keeps the filename stable across runs; the default prefixes an
            // incrementing counter, which makes captures hard to compare between builds.
            context.takeScreenshot(TestScreenshotOptions.of("client-bootstrap").disableCounterPrefix());
        }
    }
}
