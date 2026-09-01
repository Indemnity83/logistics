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
 * Renders the blocks and screens a headless server can never exercise, and captures them for review.
 *
 * <p>These are the surfaces the static resource contract cannot reach. It proves a model file
 * resolves; only a real client proves the model actually draws, that a multipart pipe picks the
 * right connection shapes, or that a screen opens at all.
 *
 * <p>Captures are review artifacts, not a pass/fail gate — see "Client feature tests" in TESTING.md
 * for why, and for the determinism steps every test here has to repeat.
 */
public class ShowcaseClientGameTest implements FabricClientGameTest {

    /**
     * Topmost solid block of the superflat test world: the classic flat preset stacks bedrock, two
     * dirt, and a grass block from the -64 world floor, so the surface sits at -61 and the first
     * free block is {@code GROUND + 1}.
     */
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

    /**
     * Time, weather, and daylight are pinned rather than left to the world's own progression, so a
     * capture taken minutes into a slow run looks identical to one taken immediately.
     */
    private void freezeWorld(TestServerContext server) {
        // MC 26 renamed these rules; the pre-26 ids parse as an unknown gamerule and the command
        // failure is swallowed, so the world would keep advancing.
        server.runCommand("gamerule advance_time false");
        server.runCommand("gamerule advance_weather false");
        server.runCommand("gamerule spawn_mobs false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
    }

    /**
     * A row of blocks covering the render paths that differ from a plain cube: a multipart pipe with
     * real connections, a cable whose blockstate is supplied per loader rather than from common, a
     * machine with a block entity renderer, and a fluid tank.
     */
    private void showcaseBlocks(
            ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
        // Pipes and cables choose their model from neighbours, so place runs rather than singletons —
        // a lone pipe would render an unconnected stub and prove nothing about connection shapes.
        server.runCommand("setblock 0 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 1 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 2 %d 0 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));
        server.runCommand("setblock 1 %d 1 logistics:pipe/copper_transport_pipe".formatted(GROUND + 1));

        server.runCommand("setblock 4 %d 0 logistics:power/copper_cable".formatted(GROUND + 1));
        server.runCommand("setblock 5 %d 0 logistics:power/copper_cable".formatted(GROUND + 1));

        server.runCommand("setblock 7 %d 0 logistics:automation/kiln".formatted(GROUND + 1));
        server.runCommand("setblock 9 %d 0 logistics:pipe/glass_tank".formatted(GROUND + 1));

        // Fixed camera: without an explicit position and angle the capture follows wherever the
        // player happened to spawn. Spectator first — a survival player teleported above ground
        // falls and settles a fraction of a block differently each run, which shifts the whole
        // frame by a sub-pixel and makes otherwise identical captures diverge.
        server.runCommand("gamemode spectator @p");
        server.runCommand("tp @p 4.5 %d.0 -6.0 0 8".formatted(GROUND + 2));
        context.waitTicks(5);

        // The HUD carries health, hunger, and hotbar state that has nothing to do with block
        // rendering but would still show up as a difference between captures. Toggled through the
        // real F1 binding rather than by poking a field, since there is no longer one to poke.
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitTick();

        singleplayer.getClientLevel().waitForChunksRender();
        context.takeScreenshot(TestScreenshotOptions.of("showcase-blocks").disableCounterPrefix());

        // Restore the HUD so a later test in this class isn't captured without it.
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitTick();
    }

    /**
     * Opens the kiln's real screen through the server's menu-opening path, so the menu, its
     * synced data, and the screen's own layout are all exercised rather than constructed by hand.
     */
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

        // Fails the test if the screen never opens, rather than silently capturing the world behind it.
        context.waitForScreen(KilnScreen.class);
        context.takeScreenshot(TestScreenshotOptions.of("kiln-screen").disableCounterPrefix());
        context.setScreen(() -> null);
    }
}
