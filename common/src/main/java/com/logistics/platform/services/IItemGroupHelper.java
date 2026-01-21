package com.logistics.platform.services;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Platform-agnostic service for creating item groups (creative tabs).
 * Abstracts FabricItemGroup and CreativeModeTab.Builder.
 */
public interface IItemGroupHelper {

    /**
     * Create an item group with the given properties.
     *
     * @param id the identifier for the group
     * @param displayName the display name
     * @param icon supplier for the icon item stack
     * @param entries consumer to populate the group's entries
     * @return the created ItemGroup
     */
    ItemGroup createGroup(
            Identifier id, Text displayName, Supplier<ItemStack> icon, Consumer<ItemGroup.Entries> entries);
}
