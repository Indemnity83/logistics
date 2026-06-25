package com.logistics.automation.sawmill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for sawmill recipes. Accepts item-id strings, tag strings ("#minecraft:oak_logs"),
 * or ingredient objects. {@code energy} is optional; {@code byproduct} carries its own chance.
 */
public class SawmillRecipeSerializer implements RecipeSerializer<SawmillRecipe> {

    public static final SawmillRecipeSerializer INSTANCE = new SawmillRecipeSerializer();

    public static final MapCodec<SawmillRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(SawmillRecipe::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE)
            .optionalFieldOf("count", SawmillRecipe.DEFAULT_INGREDIENT_COUNT)
            .forGetter(SawmillRecipe::ingredientCount),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SawmillRecipe::result),
        SawmillByproduct.CODEC.fieldOf("byproduct").forGetter(SawmillRecipe::byproduct),
        Codec.intRange(0, Integer.MAX_VALUE)
            .optionalFieldOf("energy", SawmillRecipe.DEFAULT_ENERGY)
            .forGetter(SawmillRecipe::energy)
    ).apply(i, SawmillRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, SawmillRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SawmillRecipe::ingredient,
            ByteBufCodecs.VAR_INT, SawmillRecipe::ingredientCount,
            ItemStack.STREAM_CODEC, SawmillRecipe::result,
            SawmillByproduct.STREAM_CODEC, SawmillRecipe::byproduct,
            ByteBufCodecs.VAR_INT, SawmillRecipe::energy,
            SawmillRecipe::new
        );

    private SawmillRecipeSerializer() {}

    @Override
    public MapCodec<SawmillRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SawmillRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
