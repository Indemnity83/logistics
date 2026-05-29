package com.logistics.core.lib.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemMatcher {
    private ItemMatcher() {}

    /** Registry ID string for a stack, e.g. "minecraft:diamond". */
    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** True if both stacks are non-empty and share the same item type, ignoring components. */
    public static boolean sameType(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }

    /** True if both stacks are non-empty, same type, and same data components (exact match). */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }
}
