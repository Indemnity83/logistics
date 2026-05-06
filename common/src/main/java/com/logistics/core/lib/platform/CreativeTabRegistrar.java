package com.logistics.core.lib.platform;

import com.logistics.core.lib.LogisticsCreativeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Loader-agnostic registration of creative tabs and vanilla tab modifications.
 *
 * <p>Common code calls {@code CreativeTabRegistrar.INSTANCE} during {@code initCommon()} to
 * register custom tabs and insert items into vanilla tabs. Each loader provides an implementation
 * via {@code META-INF/services/}.
 *
 * <pre>{@code
 * // common — register our tab and modify vanilla tabs
 * CreativeTabRegistrar.INSTANCE.registerTab(LOGISTICS_TAB);
 * CreativeTabRegistrar.INSTANCE.modifyTab(CreativeModeTabs.INGREDIENTS, entries -> {
 *     entries.insertBefore(Items.IRON_INGOT, ITEM.TIN_INGOT);
 * });
 *
 * // fabric implementation
 * public void registerTab(LogisticsCreativeTab tab) {
 *     Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tab.id().toIdentifier(),
 *         FabricCreativeModeTab.builder()...build());
 * }
 * public void modifyTab(ResourceKey<CreativeModeTab> tab, Consumer<TabEditor> editor) {
 *     CreativeModeTabEvents.modifyOutputEvent(tab).register(fabricEntries -> editor.accept(...));
 * }
 * }</pre>
 */
public interface CreativeTabRegistrar {
    /** Register a new custom creative tab backed by the given {@link LogisticsCreativeTab} definition. */
    void registerTab(LogisticsCreativeTab tab);

    /** Insert items into an existing vanilla creative tab. */
    void modifyTab(ResourceKey<CreativeModeTab> tab, Consumer<TabEditor> editor);

    /** Loader-agnostic access to a vanilla tab's item list during modification. */
    interface TabEditor {
        void insertAfter(ItemLike anchor, ItemLike item);
        void insertBefore(ItemLike anchor, ItemLike item);
    }

    CreativeTabRegistrar INSTANCE = ServiceLoader.load(CreativeTabRegistrar.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No CreativeTabRegistrar found"));
}
