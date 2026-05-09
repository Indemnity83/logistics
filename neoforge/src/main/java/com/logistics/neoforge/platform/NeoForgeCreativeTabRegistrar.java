package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge implementation of {@link CreativeTabRegistrar}.
 *
 * <p>Custom tab registrations are collected at startup and applied when
 * {@code BuildCreativeModeTabContentsEvent} fires. Register the mod event bus via
 * {@link #init(IEventBus)} during mod construction.
 *
 * <p>TODO: {@link #registerTab} needs NeoForge's CreativeTabRegistry or Registry API —
 * the vanilla {@code Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ...)} path
 * requires deferred registration on NeoForge.
 */
public final class NeoForgeCreativeTabRegistrar implements CreativeTabRegistrar {

    private record TabRegistration(LogisticsCreativeTab tab) {}
    private record TabModification(ResourceKey<CreativeModeTab> key, Consumer<TabEditor> editor) {}

    private final List<TabRegistration> pendingTabs = new ArrayList<>();
    private final List<TabModification> pendingModifications = new ArrayList<>();

    public void init(IEventBus modBus) {
        modBus.addListener(this::onBuildCreativeTab);
    }

    @Override
    public void registerTab(LogisticsCreativeTab tab) {
        // TODO(neoforge): use DeferredRegister for CREATIVE_MODE_TAB on NeoForge
        pendingTabs.add(new TabRegistration(tab));
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
                    throw new UnsupportedOperationException(
                            "NeoForge creative tab ordering not yet implemented; "
                                    + "wire event.insertAfter or equivalent NeoForge API");
                }

                @Override
                public void insertBefore(ItemLike anchor, ItemLike item) {
                    throw new UnsupportedOperationException(
                            "NeoForge creative tab ordering not yet implemented; "
                                    + "wire event.insertBefore or equivalent NeoForge API");
                }
            });
        }
    }
}
