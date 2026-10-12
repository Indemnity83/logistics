package com.logistics.core.lib.storage;

import net.minecraft.world.item.ItemStack;

/**
 * A {@link net.minecraft.world.Container} whose slots may hold fewer than a full stack of some
 * items. {@link ContainerItemStorage} and menu slots apply this on top of the container and item
 * stack limits.
 */
public interface SlotStackLimits {

    /** Most of {@code stack}'s item that {@code slot} holds. */
    int slotStackLimit(int slot, ItemStack stack);
}
