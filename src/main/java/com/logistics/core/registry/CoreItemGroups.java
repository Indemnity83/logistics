package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CoreItemGroups {
    @FunctionalInterface
    public interface EntryProvider {
        void addEntries(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries);
    }

    private static final List<EntryProvider> ENTRY_PROVIDERS = new ArrayList<>();

    private CoreItemGroups() {}

    public static final ItemGroup LOGISTICS_TRANSPORT = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(LogisticsMod.MOD_ID, "logistics_transport"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.logistics.transport"))
                    .icon(() -> new ItemStack(CoreBlocks.MARKER))
                    .entries((displayContext, entries) -> {
                        entries.add(CoreItems.WRENCH);
                        // Gears
                        entries.add(CoreItems.WOODEN_GEAR);
                        entries.add(CoreItems.STONE_GEAR);
                        entries.add(CoreItems.COPPER_GEAR);
                        entries.add(CoreItems.IRON_GEAR);
                        entries.add(CoreItems.GOLD_GEAR);
                        entries.add(CoreItems.DIAMOND_GEAR);
                        entries.add(CoreItems.NETHERITE_GEAR);
                        entries.add(CoreBlocks.MARKER);

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
