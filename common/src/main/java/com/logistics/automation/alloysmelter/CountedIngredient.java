package com.logistics.automation.alloysmelter;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One entry in an alloy-smelter recipe's {@code ingredients} array: an ingredient plus how many of it
 * a run consumes. Written either as a bare ingredient (item id, {@code #tag}, or vanilla object —
 * count defaults to 1) or as {@code {"id": <ingredient>, "count": N}} when a count is needed.
 */
public record CountedIngredient(Ingredient ingredient, int count) {

    private static final Codec<CountedIngredient> OBJECT = RecordCodecBuilder.create(i -> i.group(
        Ingredient.CODEC.fieldOf("id").forGetter(CountedIngredient::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
    ).apply(i, CountedIngredient::new));

    /** Accepts the {@code {"id":…, "count":N}} object form or a bare ingredient (count 1). */
    public static final Codec<CountedIngredient> CODEC = Codec.either(OBJECT, Ingredient.CODEC).xmap(
        either -> either.map(Function.identity(), ingredient -> new CountedIngredient(ingredient, 1)),
        counted -> counted.count() == 1 ? Either.right(counted.ingredient()) : Either.left(counted));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, CountedIngredient::count,
            CountedIngredient::new);
}
