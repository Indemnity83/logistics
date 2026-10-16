package com.logistics.gametest.client;

import static com.logistics.gametest.client.MachineSideTabsClientGameTest.clickRedstoneButton;
import static com.logistics.gametest.client.MachineSideTabsClientGameTest.openTab;
import static com.logistics.gametest.client.MachineSideTabsClientGameTest.tabRect;
import static com.logistics.gametest.client.SideTabsClientGameTest.moveAway;
import static com.logistics.gametest.client.SideTabsClientGameTest.screenshot;

import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.client.screen.tab.TabRect;
import com.logistics.core.lib.client.screen.tab.tabs.EnergyTab;
import com.logistics.core.lib.client.screen.tab.tabs.RedstoneTab;
import com.logistics.core.lib.compat.ClientScreenCompat;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.power.screen.FuelEngineScreen;
import com.logistics.power.screen.MagmaticEngineScreen;
import com.logistics.power.screen.ReactionEngineScreen;
import com.logistics.power.screen.SteamEngineScreen;
import com.logistics.power.screen.StirlingEngineScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Opens every engine screen and captures its side tabs, then drives the Magmatic Engine's Redstone
 * Control to IGNORED so it runs without a signal, and checks that the bufferless Steam Engine's
 * Energy tab leaves out the Stored row.
 */
public class EngineSideTabsClientGameTest implements FabricClientGameTest {

    /** Surface of the flat preset: bedrock, two dirt, and grass from -64, so blocks go at +1. */
    private static final int GROUND = -61;
    /** Engines stand this far apart along x, so none pushes energy into another. */
    private static final int SPACING = 3;

    private record Engine(String key, Class<? extends Screen> screen) {
    }

    private static final List<Engine> ENGINES = List.of(
            new Engine("stirling_engine", StirlingEngineScreen.class),
            new Engine("fuel_engine", FuelEngineScreen.class),
            new Engine("magmatic_engine", MagmaticEngineScreen.class),
            new Engine("steam_engine", SteamEngineScreen.class),
            new Engine("reaction_engine", ReactionEngineScreen.class));
    private static final int MAGMATIC = 2;
    private static final int STEAM = 3;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        // Blurring the world behind a screen stalls the lock-stepped server on a software rasteriser.
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.resizeGui();
        });

        try (TestSingleplayerContext singleplayer =
                context.worldBuilder().setUseConsistentSettings(true).create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule advance_weather false");
            server.runCommand("gamerule spawn_mobs false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode survival @p");
            for (int i = 0; i < ENGINES.size(); i++) {
                BlockPos pos = position(i);
                server.runCommand("setblock %d %d %d logistics:power/%s"
                        .formatted(pos.getX(), pos.getY(), pos.getZ(), ENGINES.get(i).key()));
            }
            singleplayer.getConnection().waitForChunksRender();

            for (int i = 0; i < ENGINES.size(); i++) {
                open(context, server, i);
                moveAway(context);
                context.waitTicks(2);
                screenshot(context, "tabs-" + ENGINES.get(i).key());
                context.setScreen(() -> null);
            }

            open(context, server, MAGMATIC);
            int bufferedEnergyHeight = magmaticEnergyTab(context);
            magmaticRedstoneIgnored(context, server);
            context.setScreen(() -> null);

            open(context, server, STEAM);
            steamEnergyTabHasNoStoredRow(context, bufferedEnergyHeight);
            context.setScreen(() -> null);
        }
    }

    private static BlockPos position(int index) {
        return new BlockPos(index * SPACING, GROUND + 1, 0);
    }

    /** Opens the {@code index}-th engine through the server's menu path and waits for the status sync. */
    private static void open(ClientGameTestContext context, TestServerContext server, int index) {
        BlockPos pos = position(index);
        server.runCommand("tp @p %d.5 %d.0 -3.0 0 20".formatted(pos.getX(), GROUND + 1));
        context.waitTicks(2);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            EngineEntity engine = engine(player, pos);
            if (!(engine instanceof MenuBehavior.HasMenu menu)) {
                throw new AssertionError("The engine at " + pos + " has no menu");
            }
            player.openMenu(menu.createMenuProvider());
        });
        context.waitForScreen(ENGINES.get(index).screen());
        context.waitFor(client -> controls().supported());
        context.waitTicks(3);
    }

    /** Opens the Magmatic Engine's Energy tab, which lists Stored, and returns its open height. */
    private static int magmaticEnergyTab(ClientGameTestContext context) {
        if (!context.computeOnClient(client -> controls().hasBuffer())) {
            throw new AssertionError("The Magmatic Engine should report an energy buffer");
        }
        openTab(context, EnergyTab.ID);
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-magmatic_engine-energy");
        return tabRect(context, EnergyTab.ID).h();
    }

    /** IGNORED through the Redstone Control tab lets the unpowered Magmatic Engine run. */
    private static void magmaticRedstoneIgnored(ClientGameTestContext context, TestServerContext server) {
        BlockPos pos = position(MAGMATIC);
        String before = server.computeOnServer(minecraftServer -> {
            EngineEntity engine = engine(player(minecraftServer), pos);
            if (engine.redstoneControl() == null || engine.redstoneControl().mode() != RedstoneMode.HIGH) {
                return "The Magmatic Engine should start in HIGH mode";
            }
            if (engine.isPowered() || engine.isEnabled()) {
                return "The unpowered Magmatic Engine should start disabled";
            }
            return null;
        });
        if (before != null) {
            throw new AssertionError(before);
        }

        openTab(context, RedstoneTab.ID);
        clickRedstoneButton(context, RedstoneMode.IGNORED);
        context.waitFor(client -> controls().mode() == RedstoneMode.IGNORED);

        String after = server.computeOnServer(minecraftServer -> {
            EngineEntity engine = engine(player(minecraftServer), pos);
            RedstoneMode mode = engine.redstoneControl().mode();
            if (mode != RedstoneMode.IGNORED) {
                return "Expected the Magmatic Engine in IGNORED mode on the server, found " + mode;
            }
            if (engine.isPowered()) {
                return "The Magmatic Engine should have no redstone signal";
            }
            if (!engine.isEnabled()) {
                return "The Magmatic Engine in IGNORED mode should be enabled without a signal";
            }
            return null;
        });
        if (after != null) {
            throw new AssertionError(after);
        }
        context.waitFor(client -> controls().enabled());
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-magmatic_engine-redstone");
    }

    /** The bufferless Steam Engine's Energy tab shows Generation and Max Output only, so it opens shorter. */
    private static void steamEnergyTabHasNoStoredRow(ClientGameTestContext context, int bufferedHeight) {
        if (context.computeOnClient(client -> controls().hasBuffer())) {
            throw new AssertionError("The Steam Engine should report no energy buffer");
        }
        openTab(context, EnergyTab.ID);
        TabRect energy = tabRect(context, EnergyTab.ID);
        if (energy.h() >= bufferedHeight) {
            throw new AssertionError("The Steam Engine's Energy tab is " + energy.h()
                    + " tall, not shorter than the buffered Magmatic Engine's " + bufferedHeight
                    + ", so it still lists Stored");
        }
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-steam_engine-energy");
    }

    /** The open engine screen's synced controls. */
    private static MachineControls controls() {
        Screen screen = ClientScreenCompat.currentScreen();
        return switch (screen) {
            case StirlingEngineScreen stirling -> stirling.getMenu().controls();
            case FuelEngineScreen fuel -> fuel.getMenu().controls();
            case MagmaticEngineScreen magmatic -> magmatic.getMenu().controls();
            case SteamEngineScreen steam -> steam.getMenu().controls();
            case ReactionEngineScreen reaction -> reaction.getMenu().controls();
            case null, default -> throw new AssertionError("Expected an engine screen, found " + screen);
        };
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static EngineEntity engine(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof EngineEntity engine)) {
            throw new AssertionError("Expected an engine at " + pos + ", found " + blockEntity);
        }
        return engine;
    }
}
