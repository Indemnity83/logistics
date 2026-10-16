package com.logistics.core.lib.client.screen.tab;

import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import com.logistics.core.machine.control.MachineControls;
import java.util.function.LongSupplier;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The standard side-tab sets for machine and engine screens. */
public final class MachineTabs {

    private MachineTabs() {
    }

    /**
     * Tabs for an automation machine screen.
     *
     * @param key the machine's key: the {@link TabMemory} screen key and the info text suffix
     * @param menu the screen's menu, for sending tab button clicks
     * @param controls the menu's synced redstone and energy state
     */
    public static SideTabHost machine(String key, AbstractContainerMenu menu, MachineControls controls, TabAnchor anchor) {
        return machine(key, menu, controls, anchor, SideTabsWidget.fontText(), TabAnimation.SYSTEM_CLOCK);
    }

    /** Tabs for an engine screen; same parameters as {@link #machine}. */
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
        return new SideTabHost(key, anchor, text, clock).add(new InfoTab(key));
    }

    static SideTabHost engine(
            String key,
            AbstractContainerMenu menu,
            MachineControls controls,
            TabAnchor anchor,
            TabText text,
            LongSupplier clock) {
        return new SideTabHost(key, anchor, text, clock).add(new InfoTab(key));
    }
}
