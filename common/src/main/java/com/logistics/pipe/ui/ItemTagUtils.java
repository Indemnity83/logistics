package com.logistics.pipe.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Shared utility for writing to an item's CUSTOM_DATA in item-mode screen handlers. */
public final class ItemTagUtils {
    private ItemTagUtils() {}

    public static void writeToItemTag(ServerPlayer player, InteractionHand hand, Consumer<CompoundTag> modifier) {
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        modifier.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.getInventory().setChanged();
    }
}
