package com.logistics.automation.alloysmelter;

import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.logistics.core.lib.recipe.MachineResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for alloy-smelter recipes. {@code input_a}/{@code input_b} accept item-id strings, tag
 * strings ("#c:ingots/tin"), or ingredient objects; their counts and {@code energy}/{@code experience}
 * are optional, and {@code byproduct} (with its own chance) is optional.
 */
public class AlloySmelterRecipeSerializer {

    public static final MapCodec<AlloySmelterRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("input_a").forGetter(AlloySmelterRecipe::inputA),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("count_a", AlloySmelterRecipe.DEFAULT_INPUT_COUNT)
            .forGetter(AlloySmelterRecipe::countA),
        Ingredient.CODEC.fieldOf("input_b").forGetter(AlloySmelterRecipe::inputB),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("count_b", AlloySmelterRecipe.DEFAULT_INPUT_COUNT)
            .forGetter(AlloySmelterRecipe::countB),
        MachineResult.CODEC.fieldOf("result").forGetter(AlloySmelterRecipe::result),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("energy", AlloySmelterRecipe.DEFAULT_ENERGY)
            .forGetter(AlloySmelterRecipe::energy),
        Codec.FLOAT
            .optionalFieldOf("experience", AlloySmelterRecipe.DEFAULT_EXPERIENCE)
            .forGetter(AlloySmelterRecipe::experience),
        AlloySmelterByproduct.CODEC.optionalFieldOf("byproduct").forGetter(AlloySmelterRecipe::byproduct)
    ).apply(i, AlloySmelterRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmelterRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AlloySmelterRecipe::inputA,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::countA,
            Ingredient.CONTENTS_STREAM_CODEC, AlloySmelterRecipe::inputB,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::countB,
            MachineResult.STREAM_CODEC, AlloySmelterRecipe::result,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipe::energy,
            ByteBufCodecs.FLOAT, AlloySmelterRecipe::experience,
            ByteBufCodecs.optional(AlloySmelterByproduct.STREAM_CODEC), AlloySmelterRecipe::byproduct,
            AlloySmelterRecipe::new
        );

    public static final RecipeSerializer<AlloySmelterRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private AlloySmelterRecipeSerializer() {}
}
