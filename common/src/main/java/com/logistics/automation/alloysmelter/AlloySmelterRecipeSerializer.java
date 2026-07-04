package com.logistics.automation.alloysmelter;

import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.logistics.core.lib.recipe.ItemResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for alloy-smelter recipes. The two inputs are an unordered {@code ingredients} array of
 * exactly two entries (each a bare ingredient or a {@code {"id":…, "count":N}} object); {@code energy}
 * and {@code experience} are optional, and {@code byproduct} (with its own chance) is optional.
 */
public class AlloySmelterRecipeSerializer {

    public static final MapCodec<AlloySmelterRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        CountedIngredient.CODEC.listOf()
            .comapFlatMap(
                list -> list.size() == 2
                    ? DataResult.success(list)
                    : DataResult.error(() -> "alloy_smelter requires exactly 2 ingredients, got " + list.size()),
                Function.identity())
            .fieldOf("ingredients")
            .forGetter(AlloySmelterRecipe::ingredientList),
        ItemResult.CODEC.fieldOf("result").forGetter(AlloySmelterRecipe::result),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("energy", AlloySmelterRecipe.DEFAULT_ENERGY)
            .forGetter(AlloySmelterRecipe::energy),
        Codec.FLOAT
            .optionalFieldOf("experience", AlloySmelterRecipe.DEFAULT_EXPERIENCE)
            .forGetter(AlloySmelterRecipe::experience),
        AlloySmelterByproduct.CODEC.optionalFieldOf("byproduct").forGetter(AlloySmelterRecipe::byproduct)
    ).apply(i, AlloySmelterRecipe::fromIngredients));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmelterRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AlloySmelterRecipe::inputA,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::countA,
            Ingredient.CONTENTS_STREAM_CODEC, AlloySmelterRecipe::inputB,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::countB,
            ItemResult.STREAM_CODEC, AlloySmelterRecipe::result,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::energy,
            ByteBufCodecs.FLOAT, AlloySmelterRecipe::experience,
            ByteBufCodecs.optional(AlloySmelterByproduct.STREAM_CODEC), AlloySmelterRecipe::byproduct,
            AlloySmelterRecipe::new
        );

    public static final RecipeSerializer<AlloySmelterRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private AlloySmelterRecipeSerializer() {}
}
