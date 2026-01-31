package com.logistics.automation.registry;

import com.logistics.core.registry.CoreItemGroups;

import net.minecraft.item.ItemGroup;

public final class AutomationItemGroups {
    private AutomationItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(AutomationItemGroups::addEntries);
    }

    private static void addEntries(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries) {
        entries.add(AutomationBlocks.QUARRY);
    }

    public static void initialize() {
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
