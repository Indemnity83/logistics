package com.logistics.gametest.client;

import static com.logistics.gametest.client.SideTabsClientGameTest.host;
import static com.logistics.gametest.client.SideTabsClientGameTest.hover;
import static com.logistics.gametest.client.SideTabsClientGameTest.moveAway;
import static com.logistics.gametest.client.SideTabsClientGameTest.screenshot;

import com.logistics.automation.alloysmelter.AlloySmelterScreen;
import com.logistics.automation.crucible.CrucibleScreen;
import com.logistics.automation.fabricator.SequentialFabricatorScreen;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.refinery.RefineryScreen;
import com.logistics.automation.sawmill.SawmillScreen;
import com.logistics.automation.transposer.TransposerScreen;
import com.logistics.core.lib.client.screen.tab.SideTabHost;
import com.logistics.core.lib.client.screen.tab.TabRect;
import com.logistics.core.lib.client.screen.tab.tabs.EnergyTab;
import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import com.logistics.core.lib.client.screen.tab.tabs.RedstoneTab;
import com.logistics.core.lib.compat.ClientScreenCompat;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Opens every automation machine screen and captures its side tabs, then drives the Macerator's
 * tabs with real mouse input: the Redstone Control buttons change the machine's mode on the server,
 * a carried item survives a click on a tab, and the recipe book hides the left side while the right
 * side keeps working.
 */
public class MachineSideTabsClientGameTest implements FabricClientGameTest {

    /** Surface of the flat preset: bedrock, two dirt, and grass from -64, so blocks go at +1. */
    private static final int GROUND = -61;
    /** Machines stand this far apart along x. */
    private static final int SPACING = 3;

    private record Machine(String key, Class<? extends Screen> screen) {
    }

    private static final List<Machine> MACHINES = List.of(
            new Machine("macerator", MaceratorScreen.class),
            new Machine("kiln", KilnScreen.class),
            new Machine("sawmill", SawmillScreen.class),
            new Machine("alloy_smelter", AlloySmelterScreen.class),
            new Machine("crucible", CrucibleScreen.class),
            new Machine("refinery", RefineryScreen.class),
            new Machine("sequential_fabricator", SequentialFabricatorScreen.class),
            new Machine("transposer", TransposerScreen.class));

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        // Blurring the world behind a screen stalls the lock-stepped server on a software rasteriser.
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));
        // 640×360 GUI pixels: wide enough that the recipe book opens beside the GUI, not over it.
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
            for (int i = 0; i < MACHINES.size(); i++) {
                BlockPos pos = position(i);
                server.runCommand("setblock %d %d %d logistics:automation/%s"
                        .formatted(pos.getX(), pos.getY(), pos.getZ(), MACHINES.get(i).key()));
            }
            singleplayer.getConnection().waitForChunksRender();

            for (int i = 0; i < MACHINES.size(); i++) {
                open(context, server, i);
                moveAway(context);
                context.waitTicks(2);
                screenshot(context, "tabs-" + MACHINES.get(i).key());
                context.setScreen(() -> null);
            }

            open(context, server, 0);
            redstoneButtonsChangeTheMode(context, server);
            energyTabOpens(context);
            carriedItemSurvivesATabClick(context, server);
            recipeBookHidesTheLeftSide(context, server);
            context.setScreen(() -> null);
        }
    }

    private static BlockPos position(int index) {
        return new BlockPos(index * SPACING, GROUND + 1, 0);
    }

    /** Opens the {@code index}-th machine through the server's menu path, half charged, and waits for the status sync. */
    private static void open(ClientGameTestContext context, TestServerContext server, int index) {
        BlockPos pos = position(index);
        server.runCommand("tp @p %d.5 %d.0 -3.0 0 20".formatted(pos.getX(), GROUND + 1));
        context.waitTicks(2);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            MachineEntity machine = machine(player, pos);
            var energy = machine.energyStorage(null);
            // Each insert is capped at the buffer's input rate, so fill to half in steps.
            while (energy.getAmount() < energy.getCapacity() / 2
                    && energy.insert(energy.getCapacity() / 2 - energy.getAmount(), false) > 0) {
                // keep inserting
            }
            player.openMenu(machine.createMenuProvider());
        });
        context.waitForScreen(MACHINES.get(index).screen());
        context.waitFor(client -> controls().supported());
        context.waitTicks(3);
    }

    private void redstoneButtonsChangeTheMode(ClientGameTestContext context, TestServerContext server) {
        RedstoneMode before = serverMode(server, 0);
        if (before == RedstoneMode.HIGH) {
            throw new AssertionError("The Macerator should not start in HIGH mode");
        }

        openTab(context, RedstoneTab.ID);
        clickRedstoneButton(context, RedstoneMode.HIGH);

        context.waitFor(client -> controls().mode() == RedstoneMode.HIGH);
        RedstoneMode after = serverMode(server, 0);
        if (after != RedstoneMode.HIGH) {
            throw new AssertionError("Expected the Macerator in HIGH mode on the server, found " + after);
        }
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-macerator-redstone");
    }

    private void energyTabOpens(ClientGameTestContext context) {
        openTab(context, EnergyTab.ID);
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-macerator-energy");
    }

    /** A click on a collapsed tab while carrying an item opens the tab and neither drops nor moves the item. */
    private void carriedItemSurvivesATabClick(ClientGameTestContext context, TestServerContext server) {
        ItemStack carried = new ItemStack(Items.DIAMOND, 5);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            player.containerMenu.setCarried(carried.copy());
            player.containerMenu.broadcastChanges();
        });
        context.waitFor(client -> ItemStack.matches(client.player.containerMenu.getCarried(), carried));

        TabRect info = tabRect(context, InfoTab.ID);
        if (context.computeOnClient(client -> host(client).isFullyOpen(InfoTab.ID))) {
            throw new AssertionError("The info tab should be collapsed before the carried-item click");
        }
        hover(context, info.x() + info.w() / 2.0, info.y() + info.h() / 2.0);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(5);

        context.waitFor(client -> host(client).isFullyOpen(InfoTab.ID));
        ItemStack clientCarried = context.computeOnClient(client -> client.player.containerMenu.getCarried().copy());
        if (!ItemStack.matches(clientCarried, carried)) {
            throw new AssertionError("The carried stack changed on the client: " + clientCarried);
        }
        String serverState = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            List<ItemEntity> dropped = player.level().getEntitiesOfClass(
                    ItemEntity.class, new AABB(player.blockPosition()).inflate(16));
            ItemStack serverCarried = player.containerMenu.getCarried();
            if (!dropped.isEmpty()) {
                return "items were dropped: " + dropped;
            }
            if (!ItemStack.matches(serverCarried, carried)) {
                return "the carried stack changed on the server: " + serverCarried;
            }
            return null;
        });
        if (serverState != null) {
            throw new AssertionError(serverState);
        }

        // Put the diamonds away so closing the screen does not throw them on the ground.
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        });
        context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty());
    }

    /** Opening the recipe book hides the left tabs; the right-side Redstone Control still changes the mode. */
    private void recipeBookHidesTheLeftSide(ClientGameTestContext context, TestServerContext server) {
        TabRect info = tabRect(context, InfoTab.ID);
        TabRect energy = tabRect(context, EnergyTab.ID);

        click(context, recipeBookButton(context));
        context.waitFor(client -> host(client).tabRect(InfoTab.ID) == null);
        context.waitFor(client -> !host(client).anyAnimating());
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-macerator-recipe-book");

        context.runOnClient(client -> {
            SideTabHost host = host(client);
            if (host.tabRect(EnergyTab.ID) != null) {
                throw new AssertionError("The energy tab is still placed while the recipe book is open");
            }
            for (TabRect rect : List.of(info, energy)) {
                if (host.isMouseOver(rect.x() + rect.w() / 2.0, rect.y() + rect.h() / 2.0)) {
                    throw new AssertionError("A hidden left tab still takes the mouse at " + rect);
                }
            }
            TabRect redstone = host.tabRect(RedstoneTab.ID);
            for (TabRect area : host.exclusionAreas()) {
                if (!area.equals(redstone)) {
                    throw new AssertionError("Unexpected exclusion area " + area + " besides the redstone tab");
                }
            }
            if (!host.isFullyOpen(RedstoneTab.ID)) {
                throw new AssertionError("The redstone tab should stay open beside the recipe book");
            }
        });

        clickRedstoneButton(context, RedstoneMode.LOW);
        context.waitFor(client -> controls().mode() == RedstoneMode.LOW);
        RedstoneMode after = serverMode(server, 0);
        if (after != RedstoneMode.LOW) {
            throw new AssertionError("Expected the Macerator in LOW mode on the server, found " + after);
        }

        // Close the book again so the remembered recipe-book state does not leak into later tests.
        click(context, recipeBookButton(context));
        context.waitFor(client -> host(client).tabRect(InfoTab.ID) != null);
    }

    /** Clicks a collapsed tab and waits until it is fully open. */
    static void openTab(ClientGameTestContext context, String id) {
        if (!context.computeOnClient(client -> host(client).isFullyOpen(id))) {
            click(context, tabRect(context, id));
        }
        context.waitFor(client -> host(client).isFullyOpen(id) && !host(client).anyAnimating());
    }

    /** Clicks the fully open redstone tab's button for {@code mode}. */
    static void clickRedstoneButton(ClientGameTestContext context, RedstoneMode mode) {
        TabRect rect = tabRect(context, RedstoneTab.ID);
        int index = RedstoneTab.BUTTON_ORDER.indexOf(mode);
        double x = rect.x() + SideTabHost.CONTENT_PAD + index * RedstoneTab.BUTTON_STRIDE + RedstoneTab.BUTTON_SIZE / 2.0;
        double y = rect.y() + SideTabHost.HEADER + RedstoneTab.BUTTON_SIZE / 2.0;
        hover(context, x, y);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    static TabRect tabRect(ClientGameTestContext context, String id) {
        TabRect rect = context.computeOnClient(client -> host(client).tabRect(id));
        if (rect == null) {
            throw new AssertionError("The " + id + " tab's side is hidden");
        }
        return rect;
    }

    /** The open machine screen's synced controls. */
    private static MachineControls controls() {
        Screen screen = ClientScreenCompat.currentScreen();
        return switch (screen) {
            case MaceratorScreen macerator -> macerator.getMenu().controls();
            case KilnScreen kiln -> kiln.getMenu().controls();
            case SawmillScreen sawmill -> sawmill.getMenu().controls();
            case AlloySmelterScreen alloySmelter -> alloySmelter.getMenu().controls();
            case CrucibleScreen crucible -> crucible.getMenu().controls();
            case RefineryScreen refinery -> refinery.getMenu().controls();
            case SequentialFabricatorScreen fabricator -> fabricator.getMenu().controls();
            case TransposerScreen transposer -> transposer.getMenu().controls();
            case null, default -> throw new AssertionError("Expected a machine screen, found " + screen);
        };
    }

    /** The recipe book toggle's current bounds; it moves with the GUI when the book opens. */
    private static TabRect recipeBookButton(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            for (var child : ClientScreenCompat.currentScreen().children()) {
                if (child instanceof ImageButton image) {
                    return new TabRect(image.getX(), image.getY(), image.getWidth(), image.getHeight());
                }
            }
            throw new AssertionError("The Macerator screen has no recipe book button");
        });
    }

    private static void click(ClientGameTestContext context, TabRect rect) {
        hover(context, rect.x() + rect.w() / 2.0, rect.y() + rect.h() / 2.0);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static RedstoneMode serverMode(TestServerContext server, int index) {
        return server.computeOnServer(minecraftServer -> {
            MachineEntity machine = machine(player(minecraftServer), position(index));
            if (machine.redstoneControl() == null) {
                throw new AssertionError("The machine at " + position(index) + " has no redstone control");
            }
            return machine.redstoneControl().mode();
        });
    }

    static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static MachineEntity machine(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof MachineEntity machine)) {
            throw new AssertionError("Expected a machine at " + pos + ", found " + blockEntity);
        }
        return machine;
    }
}
