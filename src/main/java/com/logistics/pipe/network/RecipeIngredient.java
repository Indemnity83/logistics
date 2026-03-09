package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

/**
 * A single ingredient in a crafting recipe: the item required and how many per batch.
 * Pure value object — no Minecraft world coupling.
 */
public record RecipeIngredient(ItemVariant item, long amountPerBatch) {
    public RecipeIngredient {
        if (item == null) throw new NullPointerException("item must not be null");
        if (amountPerBatch <= 0) throw new IllegalArgumentException("amountPerBatch must be positive, got: " + amountPerBatch);
    }
}
