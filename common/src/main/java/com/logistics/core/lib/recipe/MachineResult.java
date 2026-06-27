package com.logistics.core.lib.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

/**
 * Version-agnostic result of a custom-recipe machine. Wraps the vanilla result type, which differs
 * across MC versions (26.x: {@code ItemStackTemplate}; 1.21.x: plain {@code ItemStack}), so machine
 * recipe and serializer code names only {@code MachineResult} and stays byte-identical across branches.
 *
 * <p>Mirrors the {@code ResourceId} / {@code NbtCompat} compat-shim pattern: this file is the single
 * per-version implementation; the machine code above it is shared.
 */
public final class MachineResult {

    private final ItemStackTemplate template;

    private MachineResult(ItemStackTemplate template) {
        this.template = template;
    }

    /** A result of {@code count} of {@code item}. */
    public static MachineResult of(ItemLike item, int count) {
        return new MachineResult(new ItemStackTemplate(item.asItem(), count));
    }

    /** Codec for the recipe {@code "result"} field. */
    public static final Codec<MachineResult> CODEC =
            ItemStackTemplate.CODEC.xmap(MachineResult::new, r -> r.template);

    /** Network codec for syncing the result to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineResult> STREAM_CODEC =
            ItemStackTemplate.STREAM_CODEC.map(MachineResult::new, r -> r.template);

    /** A fresh result stack. */
    public ItemStack toStack() {
        return template.create();
    }

    /** The result as a recipe-book {@link SlotDisplay}. */
    public SlotDisplay slotDisplay() {
        return new SlotDisplay.ItemStackSlotDisplay(template);
    }
}
