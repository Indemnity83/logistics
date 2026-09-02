package com.logistics.gametest.client;

import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.automation.kiln.KilnScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Renders the block types whose drawing differs from a plain cube, opens a machine screen, and
 * captures both.
 */
public class ShowcaseClientGameTest implements FabricClientGameTest {

    /** Surface of the flat preset: bedrock, two dirt, and grass from -64, so blocks go at +1. */
    private static final int GROUND = -61;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();

        try (TestSingleplayerContext singleplayer =
                context.worldBuilder().setUseConsistentSettings(true).create()) {
            TestServerContext server = singleplayer.getServer();
            freezeWorld(server);

            showcaseBlocks(context, singleplayer, server);
            kilnScreenOpens(context, singleplayer, server);
        }
    }

    /** Pins time, weather, and daylight so a capture does not depend on how long the run took. */
    private void freezeWorld(TestServerContext server) {
        // MC 26 renamed these rules; the pre-26 ids parse as an unknown gamerule and the command
        // failure is swallowed, so the world would keep advancing.
        server.runCommand("gamerule advance_time false");
        server.runCommand("gamerule advance_weather false");
        server.runCommand("gamerule spawn_mobs false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
    }

    /** Multipart pipe, cable blockstate, machine renderer, and fluid tank. */
    private void showcaseBlocks(
            ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
        // Pipes and cables pick their model from neighbours; a lone one renders an unconnected stub.
        server.runCommand("setblock 0 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 1 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 2 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 1 %d 1 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));

        server.runCommand("setblock 4 %d 0 logistics:power/copper_cable".formatted(GROUND + 1));
        server.runCommand("setblock 5 %d 0 logistics:power/copper_cable".formatted(GROUND + 1));

        server.runCommand("setblock 7 %d 0 logistics:automation/kiln".formatted(GROUND + 1));
        server.runCommand("setblock 9 %d 0 logistics:pipe/glass_tank".formatted(GROUND + 1));

        // Spectator before teleporting: a survival player lands a fraction of a block differently
        // each run, shifting the whole frame by a sub-pixel.
        server.runCommand("gamemode spectator @p");
        server.runCommand("tp @p 4.5 %d.0 -6.0 0 8".formatted(GROUND + 2));
        context.waitTicks(5);

        // Hide the HUD: its health, hunger, and hotbar state varies between captures. Options has
        // no hideGui field in 26.2, so this goes through the real F1 binding.
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitTick();

        singleplayer.getClientLevel().waitForChunksRender();
        context.takeScreenshot(TestScreenshotOptions.of("showcase-blocks").disableCounterPrefix());

        // Restore the HUD so a later test in this class isn't captured without it.
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitTick();
    }

    /** Opens the kiln screen through the server's real menu path rather than constructing it. */
    private void kilnScreenOpens(
            ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
        BlockPos kiln = new BlockPos(7, GROUND + 1, 0);

        // A spectator can't open a block's menu, so switch back before asking the server to.
        server.runCommand("gamemode creative @p");

        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().get(0);
            BlockEntity blockEntity = player.level().getBlockEntity(kiln);
            if (!(blockEntity instanceof KilnBlockEntity kilnEntity)) {
                throw new AssertionError("Expected a KilnBlockEntity at " + kiln);
            }
            player.openMenu(kilnEntity.createMenuProvider());
        });

        // Fails if the screen never opens, rather than capturing the world behind it.
        context.waitForScreen(KilnScreen.class);
        context.takeScreenshot(TestScreenshotOptions.of("kiln-screen").disableCounterPrefix());
        context.setScreen(() -> null);
    }
}
