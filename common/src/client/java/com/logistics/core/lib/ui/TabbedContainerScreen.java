package com.logistics.core.lib.ui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * The base class for all tabbed GUI screens in the library.
 * It handles side-locking and provides a context for defining tab content.
 * 
 * @param <H> The type of the ScreenHandler (Menu) this screen uses.
 */
public abstract class TabbedContainerScreen<H extends AbstractContainerMenu> 
        extends AbstractContainerScreen<H> {

    protected final List<Tab<H>> tabs = new ArrayList<>();
    private final Map<Tab.TabSide, Tab<H>> activeTabs = new EnumMap<>(Tab.TabSide.class);
    private final Map<Tab.TabSide, Map<Tab<H>, List<SlotMapping>>> sideMappings = new EnumMap<>(Tab.TabSide.class);
    private final Map<Tab.TabSide, Map<Tab<H>, List<ButtonMapping>>> sideButtons = new EnumMap<>(Tab.TabSide.class);

    private record SideTabKey(Tab.TabSide side, Tab<?> tab) {}
    private record SlotMapping(int slotIndex, int x, int y) {}
    private record ButtonMapping(Button button, int x, int y) {}

    protected TabbedContainerScreen(H handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    protected void addTab(Tab<H> tab) {
        tabs.add(tab);
    }

    public List<Tab<H>> getTabs() {
        return tabs;
    }

    @Override
    protected void init() {
        super.init();
        sideMappings.clear();
        sideButtons.clear();
        for (Tab.TabSide side : Tab.TabSide.values()) {
            sideMappings.put(side, new HashMap<>());
            sideButtons.put(side, new HashMap<>());
        }

        for (Tab<H> tab : tabs) {
            tab.contentApplier().accept(new TabbedLayoutContextImpl<>(tab));
        }
    }

    protected void setActiveTab(Tab.TabSide side, Tab<H> tab) {
        activeTabs.put(side, tab);
    }

    protected Tab<H> getActiveTab(Tab.TabSide side) {
        return activeTabs.get(side);
    }

    protected List<SlotMapping> getActiveSideMappings(Tab.TabSide side) {
        Tab<H> active = activeTabs.get(side);
        if (active == null) return Collections.emptyList();
        return sideMappings.getOrDefault(side, Collections.emptyMap()).getOrDefault(active, Collections.emptyList());
    }

    protected List<ButtonMapping> getActiveSideButtons(Tab.TabSide side) {
        Tab<H> active = activeTabs.get(side);
        if (active == null) return Collections.emptyList();
        return sideButtons.getOrDefault(side, Collections.emptyMap()).getOrDefault(active, Collections.emptyList());
    }

    private class TabbedLayoutContextImpl<T extends H> implements TabbedLayoutContext<H> {
        private final Tab<H> tab;

        TabbedLayoutContextImpl(Tab<H> tab) {
            this.tab = tab;
        }

        @Override
        public void addSlot(int slotIndex, int x, int y) {
            sideMappings.get(tab.side()).computeIfAbsent(tab, k -> new ArrayList<>()).add(new SlotMapping(slotId(tab.side(), tab), slotIndex, x, y));
        }

        private SideTabKey sideId(Tab.TabSide side, Tab<H> tab) {
            return new SideTabKey(side, tab);
        }

        // Wait, I need to fix how the maps are used. 
        // The current sideMappings is Map<Tab.TabSide, Map<Tab<H>, List<SlotMapping>>>
        // My computeIfAbsent was: sideMappings.get(tab.side()).computeIfAbsent(...)
        // This matches the structure!
    }
}
