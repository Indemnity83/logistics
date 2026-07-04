package com.logistics.automation.sawmill;

import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.logistics.core.lib.recipe.ItemResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for sawmill recipes. Accepts item-id strings, tag strings ("#minecraft:oak_logs"),
 * or ingredient objects. {@code energy} is optional; {@code byproduct} carries its own chance.
 */
public class SawmillRecipeSerializer {

    public static final MapCodec<SawmillRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(SawmillRecipe::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("count", SawmillRecipe.DEFAULT_INGREDIENT_COUNT)
            .forGetter(SawmillRecipe::ingredientCount),
        ItemResult.CODEC.fieldOf("result").forGetter(SawmillRecipe::result),
        SawmillByproduct.CODEC.optionalFieldOf("byproduct").forGetter(SawmillRecipe::byproduct),
        Codec.intRange(0, Integer.MAX_VALUE)
            .optionalFieldOf("energy", SawmillRecipe.DEFAULT_ENERGY)
            .forGetter(SawmillRecipe::energy)
    ).apply(i, SawmillRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SawmillRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SawmillRecipe::ingredient,
            ByteBufCodecs.VAR_INT, SawmillRecipe::ingredientCount,
            ItemResult.STREAM_CODEC, SawmillRecipe::result,
            ByteBufCodecs.optional(SawmillByproduct.STREAM_CODEC), SawmillRecipe::byproduct,
            ByteBufCodecs.VAR_INT, SawmillRecipe::energy,
            SawmillRecipe::new
        );

    public static final RecipeSerializer<SawmillRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private SawmillRecipeSerializer() {}
}
