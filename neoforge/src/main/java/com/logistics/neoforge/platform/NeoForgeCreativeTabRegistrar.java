package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class NeoForgeCreativeTabRegistrar implements CreativeTabRegistrar {
    private record TabModification(ResourceKey<CreativeModeTab> key, Consumer<TabEditor> editor) {}

    private final List<TabModification> pendingModifications = new ArrayList<>();

    public void init(IEventBus modBus) {
        modBus.addListener(this::onBuildCreativeTab);
    }

    @Override
    public void registerTab(LogisticsCreativeTab tab) {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tab.id().toIdentifier(),
                CreativeModeTab.builder()
                        .title(tab.title())
                        .icon(tab.icon())
                        .displayItems((params, output) -> tab.populate(output))
                        .build());
    }

    @Override
    public void modifyTab(ResourceKey<CreativeModeTab> tab, Consumer<TabEditor> editor) {
        pendingModifications.add(new TabModification(tab, editor));
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        for (TabModification mod : pendingModifications) {
            if (!event.getTabKey().equals(mod.key())) continue;
            mod.editor().accept(new TabEditor() {
                @Override
                public void insertAfter(ItemLike anchor, ItemLike item) {
                    ItemStack newEntry = new ItemStack(item);
                    try {
                        event.insertAfter(new ItemStack(anchor), newEntry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    } catch (IllegalArgumentException ignored) {
                        event.accept(newEntry);
                    }
                }

                @Override
                public void insertBefore(ItemLike anchor, ItemLike item) {
                    ItemStack newEntry = new ItemStack(item);
                    try {
                        event.insertBefore(new ItemStack(anchor), newEntry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    } catch (IllegalArgumentException ignored) {
                        event.accept(newEntry);
                    }
                }
            });
        }
    }
}
