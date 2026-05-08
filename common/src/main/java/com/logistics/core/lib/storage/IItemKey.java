package com.logistics.core.lib.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Loader-agnostic identity token for an item type (ignoring count).
 *
 * <p>Implementations must provide correct {@code equals} and {@code hashCode} semantics
 * so that instances can be used as {@link java.util.Map} keys. Two keys are equal if and
 * only if they represent the same item with the same components/data.
 *
 * <p>This is one of the primary extension points for the item-storage abstraction layer.
 * Loader-specific adapters ({@code FabricItemKey}, {@code NeoForgeItemKey}) wrap the
 * native item-identity type and implement this interface.
 */
public interface IItemKey {

    /**
     * Create an {@link ItemStack} with this item type and the given count.
     *
     * @param count the desired stack size
     * @return a new stack representing this item
     */
    ItemStack toStack(int count);

    /**
     * Returns {@code true} if the given stack represents the same item type as this key
     * (same item and same components, ignoring count).
     *
     * @param stack the stack to test
     * @return {@code true} if the stack matches this key
     */
    boolean matches(ItemStack stack);
}
