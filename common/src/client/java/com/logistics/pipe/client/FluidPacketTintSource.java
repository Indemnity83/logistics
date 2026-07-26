package com.logistics.pipe.client;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint source (26.2 data-driven item-model tint system) that colors the fluid-packet window
 * (tint index 1) by its carried fluid. Referenced from {@code assets/logistics/items/pipe/fluid_packet.json}
 * and registered per loader under {@link #ID}. Both loaders share {@link FluidPacketColors}.
 */
public final class FluidPacketTintSource implements ItemTintSource {
    /** Codec id referenced by the item model definition's {@code tints} array. */
    public static final ResourceId ID = LogisticsPipe.resource("fluid_packet");

    public static final MapCodec<FluidPacketTintSource> MAP_CODEC = MapCodec.unit(new FluidPacketTintSource());

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        return FluidPacketColors.tintFor(stack, 1);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
