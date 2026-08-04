package com.logistics.pipe.item;

import net.minecraft.world.item.Item;

/**
 * Hidden, internal item that carries one configured quantum of a fluid through the item logistics
 * network. Never crafted or shown to players — the fluid provider pipe mints it and the fluid supplier
 * pipe consumes it. The carried fluid + quantum live in the {@code FLUID_PACKET} data component.
 */
public class FluidPacketItem extends Item {
    public FluidPacketItem(Properties properties) {
        super(properties);
    }
}
