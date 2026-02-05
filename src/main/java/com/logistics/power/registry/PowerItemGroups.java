package com.logistics.power.registry;

import com.logistics.core.registry.CoreItemGroups;
import net.minecraft.world.item.CreativeModeTab;

public final class PowerItemGroups {
    private PowerItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(PowerItemGroups::addEntries);
    }

    private static void addEntries(CreativeModeTab.ItemDisplayParameters displayContext, CreativeModeTab.Output entries) {
        entries.accept(PowerBlocks.REDSTONE_ENGINE);
        entries.accept(PowerBlocks.STIRLING_ENGINE);
        entries.accept(PowerBlocks.CREATIVE_ENGINE);
        entries.accept(PowerBlocks.CREATIVE_SINK);
    }

    public static void initialize() {
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
