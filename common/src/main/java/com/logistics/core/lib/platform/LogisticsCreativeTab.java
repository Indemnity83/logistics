package com.logistics.core.lib.platform;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-agnostic creative tab definition.
 *
 * <p>Holds the tab's identity (registry ID, display title, icon) and an ordered list of entries.
 * All content is declared in common code. Loader-specific code (e.g. {@code FabricCreativeTabSetup})
 * registers the actual MC creative tab and delegates display to {@link #populate}.
 *
 * <pre>{@code
 * // common — define and populate
 * public static final LogisticsCreativeTab LOGISTICS_TAB = LogisticsCreativeTab.create(
 *     LogisticsMod.modId("logistics_transport"),
 *     Component.literal("Logistics"),
 *     () -> new ItemStack(ITEM.IRON_GEAR)
 * );
 * LOGISTICS_TAB.add(ITEM.WRENCH, ITEM.IRON_GEAR);
 *
 * // fabric adapter — register once
 * FabricCreativeModeTab.builder()
 *     .title(LogisticsCore.LOGISTICS_TAB.title())
 *     .icon(LogisticsCore.LOGISTICS_TAB.icon())
 *     .displayItems((params, out) -> LogisticsCore.LOGISTICS_TAB.populate(out))
 *     .build()
 * }</pre>
 */
public final class LogisticsCreativeTab {

    private sealed interface Entry permits Entry.Add, Entry.Custom {
        record Add(List<ItemLike> items) implements Entry {}
        record Custom(Consumer<CreativeModeTab.Output> builder) implements Entry {}
    }

    private final ResourceId id;
    private final Component title;
    private final Supplier<ItemStack> icon;
    private final List<Entry> entries = new ArrayList<>();

    private LogisticsCreativeTab(ResourceId id, Component title, Supplier<ItemStack> icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    public static LogisticsCreativeTab create(ResourceId id, Component title, Supplier<ItemStack> icon) {
        return new LogisticsCreativeTab(id, title, icon);
    }

    /** Appends {@code item} to the end of this tab. */
    public LogisticsCreativeTab add(ItemLike item) {
        entries.add(new Entry.Add(List.of(item)));
        return this;
    }

    /** Appends each item individually so each can serve as an anchor for {@link #addAfter}/{@link #addBefore}. */
    public LogisticsCreativeTab add(ItemLike... items) {
        for (ItemLike item : items) {
            entries.add(new Entry.Add(List.of(item)));
        }
        return this;
    }

    /** Appends a custom entry (e.g. dynamic stacks, variants) to the end of this tab. */
    public LogisticsCreativeTab add(Consumer<CreativeModeTab.Output> builder) {
        entries.add(new Entry.Custom(builder));
        return this;
    }

    /**
     * Inserts {@code item} immediately after the last entry containing {@code anchor}.
     * Falls back to appending if {@code anchor} is not found.
     */
    public LogisticsCreativeTab addAfter(ItemLike anchor, ItemLike item) {
        int idx = lastIndexOf(anchor);
        entries.add(idx >= 0 ? idx + 1 : entries.size(), new Entry.Add(List.of(item)));
        return this;
    }

    /**
     * Inserts {@code item} immediately before the first entry containing {@code anchor}.
     * Falls back to appending if {@code anchor} is not found.
     */
    public LogisticsCreativeTab addBefore(ItemLike anchor, ItemLike item) {
        int idx = firstIndexOf(anchor);
        entries.add(idx >= 0 ? idx : entries.size(), new Entry.Add(List.of(item)));
        return this;
    }

    /** Pushes all entries into the tab output. Called by the loader's displayItems callback. */
    public void populate(CreativeModeTab.Output output) {
        for (Entry entry : entries) {
            switch (entry) {
                case Entry.Add e -> e.items().forEach(output::accept);
                case Entry.Custom e -> e.builder().accept(output);
            }
        }
    }

    public ResourceId id() { return id; }
    public Component title() { return title; }
    public Supplier<ItemStack> icon() { return icon; }

    private int firstIndexOf(ItemLike anchor) {
        Item target = anchor.asItem();
        for (int i = 0; i < entries.size(); i++) {
            if (entryContains(entries.get(i), target)) return i;
        }
        return -1;
    }

    private int lastIndexOf(ItemLike anchor) {
        Item target = anchor.asItem();
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entryContains(entries.get(i), target)) return i;
        }
        return -1;
    }

    private static boolean entryContains(Entry entry, Item target) {
        return entry instanceof Entry.Add add &&
               add.items().stream().anyMatch(il -> il.asItem() == target);
    }
}
