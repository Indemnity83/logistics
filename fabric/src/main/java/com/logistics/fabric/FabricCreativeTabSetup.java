package com.logistics.fabric;

import com.logistics.LogisticsCore;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;

public final class FabricCreativeTabSetup {
    private FabricCreativeTabSetup() {}

    public static void register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            LogisticsCore.LOGISTICS_TAB.id().toIdentifier(),
            FabricCreativeModeTab.builder()
                .title(LogisticsCore.LOGISTICS_TAB.title())
                .icon(LogisticsCore.LOGISTICS_TAB.icon())
                .displayItems((params, entries) -> LogisticsCore.LOGISTICS_TAB.populate(entries))
                .build()
        );

        LogisticsCore.addVanillaCreativeTabEntries((tab, modifier) ->
            CreativeModeTabEvents.modifyOutputEvent(tab).register(fabricEntries ->
                modifier.accept(new LogisticsCore.VanillaTabRegistrar.TabEntries() {
                    @Override
                    public void insertAfter(ItemLike anchor, ItemLike item) {
                        fabricEntries.insertAfter(anchor, item);
                    }

                    @Override
                    public void insertBefore(ItemLike anchor, ItemLike item) {
                        fabricEntries.insertBefore(anchor, item);
                    }
                })
            )
        );
    }
}
