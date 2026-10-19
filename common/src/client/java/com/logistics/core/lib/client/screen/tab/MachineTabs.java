package com.logistics.core.lib.client.screen.tab;

import com.logistics.core.lib.client.screen.tab.tabs.EnergyTab;
import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import com.logistics.core.lib.client.screen.tab.tabs.RedstoneTab;
import com.logistics.core.machine.control.MachineControls;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The standard side-tab sets for machine and engine screens. */
public final class MachineTabs {

    private MachineTabs() {
    }

    /**
     * Tabs for an automation machine screen: Info and Energy (usage) on the left, Redstone Control on
     * the right.
     *
     * @param key the machine's key: the {@link TabMemory} screen key and the info text suffix
     * @param menu the screen's menu, for sending tab button clicks
     * @param controls the menu's synced redstone and energy state
     */
    public static SideTabHost machine(String key, AbstractContainerMenu menu, MachineControls controls, TabAnchor anchor) {
        return machine(key, menu, controls, anchor, SideTabsWidget.fontText(), TabAnimation.SYSTEM_CLOCK);
    }

    /**
     * Tabs for an engine screen: Info and Energy (generation) on the left, Redstone Control on the
     * right; same parameters as {@link #machine}. Every engine with a screen has a redstone control,
     * and the tab shows unselected buttons until the status first syncs.
     */
    public static SideTabHost engine(String key, AbstractContainerMenu menu, MachineControls controls, TabAnchor anchor) {
        return engine(key, menu, controls, anchor, SideTabsWidget.fontText(), TabAnimation.SYSTEM_CLOCK);
    }

    static SideTabHost machine(
            String key,
            AbstractContainerMenu menu,
            MachineControls controls,
            TabAnchor anchor,
            TabText text,
            LongSupplier clock) {
        return machine(key, controls, sender(menu), anchor, text, clock);
    }

    /** {@link #machine} with the button sender given, for tests that cannot reach the client. */
    static SideTabHost machine(
            String key, MachineControls controls, IntPredicate sender, TabAnchor anchor, TabText text, LongSupplier clock) {
        return new SideTabHost(key, anchor, text, clock)
                .add(new InfoTab(key))
                .add(new EnergyTab(controls, EnergyTab.Mode.USAGE))
                .add(new RedstoneTab(controls, sender));
    }

    static SideTabHost engine(
            String key,
            AbstractContainerMenu menu,
            MachineControls controls,
            TabAnchor anchor,
            TabText text,
            LongSupplier clock) {
        return engine(key, controls, sender(menu), anchor, text, clock);
    }

    /** {@link #engine} with the button sender given, for tests that cannot reach the client. */
    static SideTabHost engine(
            String key, MachineControls controls, IntPredicate sender, TabAnchor anchor, TabText text, LongSupplier clock) {
        return new SideTabHost(key, anchor, text, clock)
                .add(new InfoTab(key))
                .add(new EnergyTab(controls, EnergyTab.Mode.GENERATION))
                .add(new RedstoneTab(controls, sender));
    }

    /**
     * Sends a menu-button click the way vanilla screens do: the client menu validates it first, and
     * only a click it accepts goes to the server.
     */
    private static IntPredicate sender(AbstractContainerMenu menu) {
        return id -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null && minecraft.gameMode != null && menu.clickMenuButton(player, id)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                return true;
            }
            return false;
        };
    }
}
