package com.logistics.gametest.client;

import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.automation.transposer.TransposerScreen;
import com.logistics.core.lib.client.screen.tab.SideTabHost;
import com.logistics.core.lib.client.screen.tab.SideTabScreen;
import com.logistics.core.lib.client.screen.tab.TabRect;
import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import com.logistics.core.lib.compat.ClientScreenCompat;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the side tabs on a real machine screen with real mouse input, and captures them collapsed,
 * hovered, opening and open, at the default GUI scale and again at scale 4.
 */
public class SideTabsClientGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/SideTabsClientGameTest");

    /** Surface of the flat preset: bedrock, two dirt, and grass from -64, so blocks go at +1. */
    private static final int GROUND = -61;
    private static final BlockPos TRANSPOSER = new BlockPos(0, GROUND + 1, 0);
    /** How long after the click the mid-animation frame is captured. */
    private static final long OPENING_CAPTURE_MS = 80L;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        // Blurring the world behind a screen stalls the lock-stepped server on a software rasteriser.
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));

        try (TestSingleplayerContext singleplayer =
                context.worldBuilder().setUseConsistentSettings(true).create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule advance_weather false");
            server.runCommand("gamerule spawn_mobs false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("setblock %d %d %d logistics:automation/transposer"
                    .formatted(TRANSPOSER.getX(), TRANSPOSER.getY(), TRANSPOSER.getZ()));
            server.runCommand("tp @p 0.5 %d.0 -3.0 0 20".formatted(GROUND + 1));
            singleplayer.getConnection().waitForChunksRender();

            openTransposer(context, server);
            defaultScale(context);
            scaleFour(context);
            context.setScreen(() -> null);
        }
    }

    private void openTransposer(ClientGameTestContext context, TestServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().get(0);
            BlockEntity blockEntity = player.level().getBlockEntity(TRANSPOSER);
            if (!(blockEntity instanceof TransposerBlockEntity transposer)) {
                throw new AssertionError("Expected a TransposerBlockEntity at " + TRANSPOSER);
            }
            var energy = transposer.energyStorage(null);
            // Each insert is capped at the buffer's input rate, so fill to half in steps.
            while (energy.getAmount() < energy.getCapacity() / 2
                    && energy.insert(energy.getCapacity() / 2 - energy.getAmount(), false) > 0) {
                // keep inserting
            }
            player.openMenu(transposer.createMenuProvider());
        });
        context.waitForScreen(TransposerScreen.class);
        context.waitTicks(5);
    }

    private void defaultScale(ClientGameTestContext context) {
        moveAway(context);
        screenshot(context, "tabs-collapsed");

        TabRect info = infoRect(context);
        hover(context, info.x() + info.w() / 2.0, info.y() + info.h() / 2.0);
        screenshot(context, "tabs-hover-info");

        long clickedAt = System.nanoTime();
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        while ((System.nanoTime() - clickedAt) / 1_000_000L < OPENING_CAPTURE_MS && !infoFullyOpen(context)) {
            context.waitTick();
        }
        if (!infoFullyOpen(context)) {
            screenshot(context, "tabs-info-opening");
        }

        context.waitFor(client -> host(client).isFullyOpen(InfoTab.ID));
        moveAway(context);
        context.waitTicks(5);
        screenshot(context, "tabs-info-open");
    }

    /**
     * Scale 4 in a window just large enough for it (1280×960 → 320×240 GUI pixels): 70px of margin
     * per side, so the remembered info tab reopens at its narrowest, with its title ellipsized.
     */
    private void scaleFour(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 960);
        context.runOnClient(client -> {
            client.options.guiScale().set(4);
            client.resizeGui();
        });
        context.waitTicks(5);
        String gui = context.computeOnClient(client -> {
            Window window = client.getWindow();
            return window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + " scale " + window.getGuiScale();
        });
        LOGGER.info("Scale-4 capture at {}", gui);
        context.waitFor(client -> host(client).isFullyOpen(InfoTab.ID));
        moveAway(context);
        screenshot(context, "tabs-scale4-info-open");

        // Collapse through the header, then hover the collapsed tab for its tooltip.
        TabRect open = infoRect(context);
        hover(context, open.x() + open.w() / 2.0, open.y() + 4);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> !host(client).anyAnimating());
        TabRect collapsed = infoRect(context);
        hover(context, collapsed.x() + collapsed.w() / 2.0, collapsed.y() + collapsed.h() / 2.0);
        context.waitTicks(2);
        screenshot(context, "tabs-scale4-hover");
    }

    static SideTabHost host(Minecraft client) {
        Screen current = ClientScreenCompat.currentScreen();
        if (!(current instanceof SideTabScreen screen)) {
            throw new AssertionError("Expected a side-tab screen, found " + current);
        }
        return screen.sideTabs();
    }

    private static boolean infoFullyOpen(ClientGameTestContext context) {
        return context.computeOnClient(client -> host(client).isFullyOpen(InfoTab.ID));
    }

    private static TabRect infoRect(ClientGameTestContext context) {
        TabRect rect = context.computeOnClient(client -> host(client).tabRect(InfoTab.ID));
        if (rect == null) {
            throw new AssertionError("The info tab's side is hidden");
        }
        return rect;
    }

    /** Moves the cursor to a point in GUI-scaled coordinates, converted to window coordinates. */
    static void hover(ClientGameTestContext context, double guiX, double guiY) {
        double[] window = context.computeOnClient(client -> {
            Window w = client.getWindow();
            double perX = (double) w.getScreenWidth() / w.getGuiScaledWidth();
            double perY = (double) w.getScreenHeight() / w.getGuiScaledHeight();
            return new double[] {guiX * perX, guiY * perY};
        });
        context.getInput().setCursorPos(window[0], window[1]);
        context.waitTick();
    }

    /** Parks the cursor in the top-left corner, clear of the GUI and of every tab. */
    static void moveAway(ClientGameTestContext context) {
        hover(context, 1, 1);
    }

    static void screenshot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }
}
