package com.logistics.fabric;

import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public final class FabricCreativeTabRegistrar implements CreativeTabRegistrar {
    @Override
    public void registerTab(LogisticsCreativeTab tab) {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            tab.id().toIdentifier(),
            FabricCreativeModeTab.builder()
                .title(tab.title())
                .icon(tab.icon())
                .displayItems((params, entries) -> tab.populate(entries))
                .build()
        );
    }

    @Override
    public void modifyTab(ResourceKey<CreativeModeTab> tab, Consumer<TabEditor> editor) {
        CreativeModeTabEvents.modifyOutputEvent(tab).register(fabricEntries ->
            editor.accept(new TabEditor() {
                @Override
                public void insertAfter(ItemLike anchor, ItemLike item) {
                    fabricEntries.insertAfter(anchor, item);
                }

                @Override
                public void insertBefore(ItemLike anchor, ItemLike item) {
                    fabricEntries.insertBefore(anchor, item);
                }
            })
        );
    }
}
