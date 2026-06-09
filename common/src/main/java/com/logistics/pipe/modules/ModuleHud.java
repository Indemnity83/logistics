package com.logistics.pipe.modules;

import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    static Component summary(String labelKey, String value) {
        return summary(labelKey, Component.literal(value));
    }

    /** A muted detail line (shown when the details key is held). */
    static Component detail(Component value) {
        return value.copy().withStyle(ChatFormatting.GRAY);
    }

    /** Resolves a stored item id ("minecraft:diamond") to its display name, falling back to the raw id. */
    static Component itemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Component.empty();
        }
        ResourceId resource = ResourceId.tryParse(itemId);
        if (resource != null) {
            var itemOpt = BuiltInRegistries.ITEM.get(resource.toIdentifier());
            if (itemOpt.isPresent() && itemOpt.get().value() != Items.AIR) {
                return new ItemStack(itemOpt.get().value()).getHoverName();
            }
        }
        return Component.literal(itemId);
    }

    /** Comma-joined display names for a list of item ids (blank ids skipped). */
    static MutableComponent itemNames(List<String> ids) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (String id : ids) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            out.append(itemName(id));
            first = false;
        }
        return out;
    }
}
