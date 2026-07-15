package com.logistics.automation.fabricator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An {@link Ingredient} paired with the quantity one fabricator craft consumes. Accepts item-id
 * strings, tag strings ({@code "#minecraft:planks"}), or ingredient objects; {@code count} defaults
 * to 1.
 */
public record SizedIngredient(Ingredient ingredient, int count) {

    public static final Codec<SizedIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(SizedIngredient::count)
    ).apply(i, SizedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
                    ByteBufCodecs.VAR_INT, SizedIngredient::count,
                    SizedIngredient::new);

    public boolean test(ItemStack stack) {
        return ingredient.test(stack);
    }
}
