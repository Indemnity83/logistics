package com.logistics.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Starts a real client, builds a world, renders it, and captures a screenshot.
 *
 * <p>Carries no {@code @GameTest} methods: the Fabric client test framework runs the whole class as
 * one script, discovered through the {@code fabric-client-gametest} entrypoint.
 */
public class ClientBootstrapGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        // Resets GUI scale, render distance, and graphics options, so a developer's local settings
        // can't change what gets captured.
        context.restoreDefaultGameOptions();

        // The harness advances the integrated server in lock-step with the client through a
        // Phaser, so the server cannot tick faster than frames are drawn. Minecraft blurs the
        // world behind an open screen -- a full-screen post-process that costs nothing on a GPU
        // and, on a software rasteriser, takes long enough that world loading times out.
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));

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
