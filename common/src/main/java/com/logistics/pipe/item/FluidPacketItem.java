package com.logistics.pipe.item;

import net.minecraft.world.item.Item;

/**
 * Hidden, internal item that carries a fluid packet through the item logistics network. Never crafted
 * or shown to players — the fluid provider pipe mints it and the fluid supplier pipe consumes it. The
 * carried fluid + amount live in the {@code FLUID_PACKET} data component.
 *
 * <p>Deliberately never stacks (max stack size 1, set at registration): fluid should move visibly and
 * at a paced rate through the network, one physical packet per item/entity, rather than disappearing
 * into large hidden item stacks.
 */
public class FluidPacketItem extends Item {
    public FluidPacketItem(Properties properties) {
        super(properties);
    }
}
