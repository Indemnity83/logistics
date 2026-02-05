package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class CoreItemGroups {
    @FunctionalInterface
    public interface EntryProvider {
        void addEntries(CreativeModeTab.ItemDisplayParameters displayContext, CreativeModeTab.Output entries);
    }

    private static final List<EntryProvider> ENTRY_PROVIDERS = new ArrayList<>();

    private CoreItemGroups() {}

    public static final CreativeModeTab LOGISTICS_TRANSPORT = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "logistics_transport"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.logistics.transport"))
                    .icon(() -> new ItemStack(CoreBlocks.MARKER))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(CoreItems.WRENCH);
                        entries.accept(CoreItems.PROBE);
                        // Gears
                        entries.accept(CoreItems.WOODEN_GEAR);
                        entries.accept(CoreItems.STONE_GEAR);
                        entries.accept(CoreItems.COPPER_GEAR);
                        entries.accept(CoreItems.IRON_GEAR);
                        entries.accept(CoreItems.GOLD_GEAR);
                        entries.accept(CoreItems.DIAMOND_GEAR);
                        entries.accept(CoreItems.NETHERITE_GEAR);
                        entries.accept(CoreBlocks.MARKER);

                        for (EntryProvider provider : ENTRY_PROVIDERS) {
                            provider.addEntries(displayContext, entries);
                        }
                    })
                    .build());

    public static void registerEntries(EntryProvider provider) {
        ENTRY_PROVIDERS.add(provider);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core item groups");
    }
}
