package com.logistics.core.lib.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Version-agnostic item result of a custom-recipe machine. Wraps the vanilla result type, which differs
 * across MC versions (26.x: {@code ItemStackTemplate}; 1.21.x: plain {@code ItemStack}), so machine
 * recipe and serializer code names only {@code ItemResult} and stays byte-identical across branches.
 *
 * <p>Mirrors the {@code ResourceId} / {@code NbtCompat} compat-shim pattern: this file is the single
 * per-version implementation; the machine code above it is shared. (1.21.1 predates the
 * {@code crafting.display} package, so this version omits {@code slotDisplay()}.)
 */
public final class ItemResult {

    private final ItemStack result;

    private ItemResult(ItemStack result) {
        this.result = result;
    }

    /** A result of {@code count} of {@code item}. */
    public static ItemResult of(ItemLike item, int count) {
        return new ItemResult(new ItemStack(item, count));
    }

    /** Codec for the recipe {@code "result"} field. */
    public static final Codec<ItemResult> CODEC =
            ItemStack.STRICT_CODEC.xmap(ItemResult::new, r -> r.result);

    /** Network codec for syncing the result to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemResult> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ItemResult::new, r -> r.result);

    /** A fresh result stack. */
    public ItemStack toStack() {
        return result.copy();
    }
}
