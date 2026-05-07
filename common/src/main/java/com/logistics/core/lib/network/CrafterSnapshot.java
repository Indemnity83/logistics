package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Immutable snapshot of a crafting pipe's state at the moment of capture.
 * Contains the output item, the per-batch output count, the recipe ingredients,
 * and the autocrafter's current buffer capacity.
 *
 * <p>Pure value object — no Minecraft world coupling beyond BlockPos/IItemKey.
 */
public record CrafterSnapshot(
        BlockPos pos,
        IItemKey output,
        long outputCount,
        List<RecipeIngredient> ingredients,
        CrafterBufferState buffer) {

    public CrafterSnapshot {
        if (pos == null) throw new NullPointerException("pos must not be null");
        if (output == null) throw new NullPointerException("output must not be null");
        if (outputCount <= 0) throw new IllegalArgumentException("outputCount must be positive, got: " + outputCount);
        if (buffer == null) throw new NullPointerException("buffer must not be null");
        ingredients = List.copyOf(ingredients); // defensive immutable copy
    }

    /** How many complete batches can be submitted right now given buffer capacity. */
    public int availableBatchCapacity() {
        return buffer.safeBatchCapacity();
    }
}
