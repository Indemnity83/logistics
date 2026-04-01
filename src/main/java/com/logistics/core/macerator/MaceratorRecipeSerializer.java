package com.logistics.core.macerator;

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
 * Recipe serializer for macerator recipes.
 * Handles bare item ID strings ("minecraft:iron_ore") and standard ingredient objects.
 * Note: {"tag":"..."} object format is not supported by vanilla's Ingredient.CODEC;
 * those recipes are handled exclusively by MaceratorRecipeManager.
 */
public class MaceratorRecipeSerializer implements RecipeSerializer<MaceratorRecipeWrapper> {

    private static final MapCodec<MaceratorRecipeWrapper> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(MaceratorRecipeWrapper::ingredient),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MaceratorRecipeWrapper::result),
        Codec.INT.fieldOf("grindingtime").forGetter(MaceratorRecipeWrapper::grindingTime),
        Codec.FLOAT.optionalFieldOf("experience", MaceratorRecipe.DEFAULT_EXPERIENCE).forGetter(MaceratorRecipeWrapper::experience)
    ).apply(i, MaceratorRecipeWrapper::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipeWrapper> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MaceratorRecipeWrapper::ingredient,
            ItemStack.STREAM_CODEC, MaceratorRecipeWrapper::result,
            ByteBufCodecs.VAR_INT, MaceratorRecipeWrapper::grindingTime,
            ByteBufCodecs.FLOAT, MaceratorRecipeWrapper::experience,
            MaceratorRecipeWrapper::new
        );

    @Override
    public MapCodec<MaceratorRecipeWrapper> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipeWrapper> streamCodec() {
        return STREAM_CODEC;
    }
}
