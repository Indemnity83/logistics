package com.logistics.gametest.client;

import static com.logistics.gametest.client.MachineSideTabsClientGameTest.openTab;
import static com.logistics.gametest.client.MachineSideTabsClientGameTest.player;
import static com.logistics.gametest.client.MachineSideTabsClientGameTest.tabRect;
import static com.logistics.gametest.client.SideTabsClientGameTest.GROUND;
import static com.logistics.gametest.client.SideTabsClientGameTest.TRANSPOSER;
import static com.logistics.gametest.client.SideTabsClientGameTest.host;
import static com.logistics.gametest.client.SideTabsClientGameTest.hover;
import static com.logistics.gametest.client.SideTabsClientGameTest.moveAway;
import static com.logistics.gametest.client.SideTabsClientGameTest.openTransposer;
import static com.logistics.gametest.client.SideTabsClientGameTest.screenshot;

import com.logistics.core.lib.client.screen.tab.SideTabHost;
import com.logistics.core.lib.client.screen.tab.SideTabScreen;
import com.logistics.core.lib.client.screen.tab.SideTabsWidget;
import com.logistics.core.lib.client.screen.tab.TabMemory;
import com.logistics.core.lib.client.screen.tab.TabRect;
import com.logistics.core.lib.client.screen.tab.TabSide;
import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import com.logistics.core.lib.compat.ClientScreenCompat;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the Transposer's side tabs through the awkward cases: rapid clicks while a tab animates,
 * windows too small to open or even show a side, and a carried item clicked where a hidden tab used
 * to be.
 */
public class SideTabsInteractionClientGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/SideTabsInteractionClientGameTest");

    /** Clicks in the rapid-toggle burst; odd, so the burst ends with the tab intended open. */
    private static final int RAPID_CLICKS = 5;
    /** Roomy layout: 640×360 GUI pixels at scale 2. */
    private static final int WIDE_W = 1280;
    private static final int WIDE_H = 720;
    private static final int WIDE_SCALE = 2;
    /** Scale 1 at 300×300: a collapsed tab fits beside the 176px GUI, the open Info tab does not. */
    private static final int SQUEEZED_W = 300;
    /** Scale 1 at 200×300: under 24px beside the GUI, so both sides hide. */
    private static final int HIDDEN_W = 200;
    private static final int SMALL_H = 300;
    private static final int SMALL_SCALE = 1;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        // Blurring the world behind a screen stalls the lock-stepped server on a software rasteriser.
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));
        int[] original = context.computeOnClient(client -> new int[] {
            client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight(), client.options.guiScale().get()
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
            server.runCommand("setblock %d %d %d logistics:automation/transposer"
                    .formatted(TRANSPOSER.getX(), TRANSPOSER.getY(), TRANSPOSER.getZ()));
            server.runCommand("tp @p 0.5 %d.0 -3.0 0 20".formatted(GROUND + 1));
            singleplayer.getClientLevel().waitForChunksRender();

            // Start from no remembered intent, whatever earlier tests left behind.
            context.runOnClient(client -> TabMemory.clear());
            resize(context, WIDE_W, WIDE_H, WIDE_SCALE);
            openTransposer(context, server);

            rapidTogglingSettles(context);
            resizeRecoversIntent(context);
            hiddenSideLeavesClicksToVanilla(context, server);

            context.setScreen(() -> null);
        } finally {
            resize(context, original[0], original[1], original[2]);
            context.runOnClient(client -> TabMemory.clear());
        }
    }

    /**
     * A burst of clicks on the same spot while the Info tab animates ends in the state the click
     * count implies, with the intent, the effective state and the rect in agreement.
     */
    private void rapidTogglingSettles(ClientGameTestContext context) {
        TabRect collapsed = tabRect(context, InfoTab.ID);
        assertSize(collapsed, SideTabHost.COLLAPSED, SideTabHost.COLLAPSED, "collapsed info tab");
        openTab(context, InfoTab.ID);
        TabRect open = tabRect(context, InfoTab.ID);
        collapseThroughHeader(context);

        // The collapsed tab's centre stays inside the header strip at every animation size, because a
        // left tab grows leftward from a fixed right edge.
        double x = collapsed.x() + collapsed.w() / 2.0;
        double y = collapsed.y() + collapsed.h() / 2.0;
        hover(context, x, y);
        int midAnimation = 0;
        for (int i = 0; i < RAPID_CLICKS; i++) {
            if (i > 0 && context.computeOnClient(client -> host(client).anyAnimating())) {
                midAnimation++;
            }
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTick();
        }
        LOGGER.info("{} of {} rapid clicks landed while the info tab was animating", midAnimation, RAPID_CLICKS - 1);

        context.waitFor(client -> !host(client).anyAnimating());
        assertSettled(context, true, collapsed, open);

        // The GUI still responds: one more click collapses it.
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> !host(client).anyAnimating());
        assertSettled(context, false, collapsed, open);
        moveAway(context);
    }

    /**
     * Squeezing the window keeps the intended tab collapsed or hidden and brings it back open when
     * room returns; a tab the player collapsed stays collapsed. The widget is never duplicated.
     */
    private void resizeRecoversIntent(ClientGameTestContext context) {
        openTab(context, InfoTab.ID);

        resize(context, SQUEEZED_W, SMALL_H, SMALL_SCALE);
        context.runOnClient(client -> {
            SideTabHost host = host(client);
            TabRect rect = host.tabRect(InfoTab.ID);
            if (rect == null) {
                throw new AssertionError("The left side should still show collapsed tabs at " + SQUEEZED_W + " wide");
            }
            assertSize(rect, SideTabHost.COLLAPSED, SideTabHost.COLLAPSED, "squeezed info tab");
            assertOpenState(host, false, InfoTab.ID, "squeezed");
        });

        resize(context, HIDDEN_W, SMALL_H, SMALL_SCALE);
        context.runOnClient(client -> {
            SideTabHost host = host(client);
            if (host.tabRect(InfoTab.ID) != null) {
                throw new AssertionError("The left side should be hidden at " + HIDDEN_W + " wide");
            }
            assertOpenState(host, false, InfoTab.ID, "hidden");
        });
        moveAway(context);
        context.waitTicks(2);
        screenshot(context, "tabs-shrunk");

        resize(context, WIDE_W, WIDE_H, WIDE_SCALE);
        context.waitFor(client -> host(client).isFullyOpen(InfoTab.ID));
        context.runOnClient(client -> assertOpenState(host(client), true, InfoTab.ID, "regrown"));

        collapseThroughHeader(context);
        resize(context, HIDDEN_W, SMALL_H, SMALL_SCALE);
        resize(context, WIDE_W, WIDE_H, WIDE_SCALE);
        context.waitFor(client -> !host(client).anyAnimating());
        context.runOnClient(client -> {
            SideTabHost host = host(client);
            assertOpenState(host, false, null, "regrown after collapsing");
            assertSize(Objects.requireNonNull(host.tabRect(InfoTab.ID)),
                    SideTabHost.COLLAPSED, SideTabHost.COLLAPSED, "regrown collapsed info tab");
        });
    }

    /**
     * With the left side hidden, a click carrying an item where the Info tab used to be is not taken
     * by the tabs: it has the same outcome as a click outside the GUI where no tab ever was, and no
     * slot changes.
     */
    private void hiddenSideLeavesClicksToVanilla(ClientGameTestContext context, TestServerContext server) {
        // The collapsed tab's spot in the squeezed window, carried over to the narrower one: the GUI
        // stays centred and the height is unchanged, so only x shifts.
        resize(context, SQUEEZED_W, SMALL_H, SMALL_SCALE);
        TabRect squeezed = tabRect(context, InfoTab.ID);
        resize(context, HIDDEN_W, SMALL_H, SMALL_SCALE);
        double formerX = squeezed.x() + squeezed.w() / 2.0 - (SQUEEZED_W - HIDDEN_W) / 2.0;
        double formerY = squeezed.y() + squeezed.h() / 2.0;
        if (formerX < 0) {
            throw new AssertionError("The former info tab position is off screen: " + formerX);
        }

        context.runOnClient(client -> {
            SideTabHost host = host(client);
            if (host.tabRect(InfoTab.ID) != null) {
                throw new AssertionError("The left side should be hidden at " + HIDDEN_W + " wide");
            }
            if (host.isMouseOver(formerX, formerY)) {
                throw new AssertionError("The hidden info tab still takes the mouse at " + formerX + "," + formerY);
            }
            if (!host.clickedOutside(true, formerX, formerY)) {
                throw new AssertionError("The hidden info tab still counts as inside the GUI");
            }
            if (!host.exclusionAreas().isEmpty()) {
                throw new AssertionError("Hidden sides still report exclusion areas: " + host.exclusionAreas());
            }
        });

        // Above the GUI, centred: outside it, and never under a tab.
        double referenceX = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth() / 2.0);
        double referenceY = 2;

        ClickOutcome atFormerTab = carriedClick(context, server, formerX, formerY);
        ClickOutcome outsideGui = carriedClick(context, server, referenceX, referenceY);
        LOGGER.info("Carried click at the hidden tab: {}; outside the GUI: {}", atFormerTab, outsideGui);
        if (!atFormerTab.equals(outsideGui)) {
            throw new AssertionError("A click at the hidden info tab (" + atFormerTab
                    + ") differs from a click outside the GUI (" + outsideGui + ")");
        }

        resize(context, WIDE_W, WIDE_H, WIDE_SCALE);
    }

    private record ClickOutcome(int carriedCount, int droppedCount) {
    }

    /** Clicks at a GUI point carrying diamonds, checks no slot changed, and cleans up after the click. */
    private ClickOutcome carriedClick(ClientGameTestContext context, TestServerContext server, double x, double y) {
        ItemStack carried = new ItemStack(Items.DIAMOND, 5);
        List<ItemStack> before = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            player.containerMenu.setCarried(carried.copy());
            player.containerMenu.broadcastChanges();
            return slots(player);
        });
        context.waitFor(client -> ItemStack.matches(client.player.containerMenu.getCarried(), carried));

        hover(context, x, y);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(5);

        ClickOutcome outcome = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            List<ItemStack> after = slots(player);
            for (int i = 0; i < before.size(); i++) {
                if (!ItemStack.matches(before.get(i), after.get(i))) {
                    throw new AssertionError("Slot " + i + " changed from " + before.get(i) + " to " + after.get(i));
                }
            }
            int dropped = 0;
            for (ItemEntity entity : player.level().getEntitiesOfClass(
                    ItemEntity.class, new AABB(player.blockPosition()).inflate(16))) {
                dropped += entity.getItem().getCount();
            }
            return new ClickOutcome(player.containerMenu.getCarried().getCount(), dropped);
        });

        server.runCommand("kill @e[type=item]");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = player(minecraftServer);
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        });
        context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty());
        moveAway(context);
        return outcome;
    }

    private static List<ItemStack> slots(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var slot : player.containerMenu.slots) {
            stacks.add(slot.getItem().copy());
        }
        return stacks;
    }

    /** Clicks the open Info tab's header and waits until it has finished collapsing. */
    private static void collapseThroughHeader(ClientGameTestContext context) {
        TabRect open = tabRect(context, InfoTab.ID);
        hover(context, open.x() + open.w() / 2.0, open.y() + 4);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> !host(client).anyAnimating() && host(client).openIntent(TabSide.LEFT) == null);
    }

    /** The settled Info tab: intent, effective state and rect agree with {@code expectOpen}. */
    private static void assertSettled(ClientGameTestContext context, boolean expectOpen, TabRect collapsed, TabRect open) {
        context.runOnClient(client -> {
            SideTabHost host = host(client);
            assertOpenState(host, expectOpen, expectOpen ? InfoTab.ID : null, "settled");
            TabRect rect = host.tabRect(InfoTab.ID);
            TabRect expected = expectOpen ? open : collapsed;
            if (!expected.equals(rect)) {
                throw new AssertionError("Expected the settled info tab at " + expected + ", found " + rect);
            }
        });
    }

    private static void assertOpenState(SideTabHost host, boolean fullyOpen, String intent, String when) {
        if (host.isFullyOpen(InfoTab.ID) != fullyOpen) {
            throw new AssertionError("Expected the info tab " + (fullyOpen ? "open" : "not open") + " when " + when);
        }
        if (!Objects.equals(host.openIntent(TabSide.LEFT), intent)) {
            throw new AssertionError("Expected left intent " + intent + " when " + when
                    + ", found " + host.openIntent(TabSide.LEFT));
        }
    }

    private static void assertSize(TabRect rect, int w, int h, String what) {
        if (rect.w() != w || rect.h() != h) {
            throw new AssertionError("Expected the " + what + " at " + w + "x" + h + ", found " + rect);
        }
    }

    /**
     * Resizes the window and GUI scale, re-initialising the open screen as a player resize does, and
     * checks the screen still holds exactly one tab widget.
     */
    private static void resize(ClientGameTestContext context, int width, int height, int scale) {
        context.getInput().resizeWindow(width, height);
        context.runOnClient(client -> {
            client.options.guiScale().set(scale);
            client.resizeGui();
        });
        context.waitTicks(2);
        String gui = context.computeOnClient(client -> {
            Window window = client.getWindow();
            return window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + " scale " + window.getGuiScale();
        });
        LOGGER.info("Resized to {}x{}: GUI {}", width, height, gui);
        context.runOnClient(client -> {
            var screen = ClientScreenCompat.currentScreen();
            if (!(screen instanceof SideTabScreen)) {
                return;
            }
            long widgets = screen.children().stream().filter(SideTabsWidget.class::isInstance).count();
            if (widgets != 1) {
                throw new AssertionError("Expected one side tab widget after a resize, found " + widgets);
            }
        });
    }
}
