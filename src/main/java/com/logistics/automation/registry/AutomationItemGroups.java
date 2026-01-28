package com.logistics.automation.registry;

import com.logistics.core.registry.CoreItemGroups;
import net.minecraft.world.item.CreativeModeTab;

public final class AutomationItemGroups {
    private AutomationItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(AutomationItemGroups::addEntries);
    }

    private static void addEntries(
            CreativeModeTab.ItemDisplayParameters displayContext, CreativeModeTab.Output entries) {
        entries.accept(AutomationBlocks.QUARRY);
    }

    public static void initialize() {
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
