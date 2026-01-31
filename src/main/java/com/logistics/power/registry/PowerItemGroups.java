package com.logistics.power.registry;

import com.logistics.core.registry.CoreItemGroups;

import net.minecraft.item.ItemGroup;

public final class PowerItemGroups {
    private PowerItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(PowerItemGroups::addEntries);
    }

    private static void addEntries(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries) {
        entries.add(PowerBlocks.REDSTONE_ENGINE);
        entries.add(PowerBlocks.STIRLING_ENGINE);
        entries.add(PowerBlocks.CREATIVE_ENGINE);
        entries.add(PowerItems.ENGINE_PROBE);
    }

    public static void initialize() {
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
