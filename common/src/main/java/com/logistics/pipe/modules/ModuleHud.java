package com.logistics.pipe.modules;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared formatting helpers for the pipe modules' Jade HUD lines ({@code Module.appendHud}). Keeps the
 * per-module readouts terse and consistent.
 */
final class ModuleHud {

    private ModuleHud() {}

    /** A "Label: value" line with a translatable label and a white value. */
    static Component summary(String labelKey, Component value) {
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    /** A muted detail line (shown when the details key is held). */
    static Component detail(Component value) {
        return value.copy().withStyle(ChatFormatting.GRAY);
    }

    /** Resolves a stored item id + amount to an {@link ItemStack} (count drives the icon overlay). */
    static ItemStack stack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceId resource = ResourceId.tryParse(itemId);
        if (resource != null) {
            Item item = BuiltInRegistries.ITEM.get(resource.toIdentifier());
            if (item != Items.AIR) {
                return new ItemStack(item, Math.max(1, count));
            }
        }
        return ItemStack.EMPTY;
    }
}
