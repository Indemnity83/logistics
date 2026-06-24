package com.logistics.core.macerator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for macerator recipes.
 * Handles bare item ID strings ("minecraft:iron_ore"), tag strings ("#minecraft:logs"),
 * and standard ingredient objects via vanilla's {@link Ingredient#CODEC}.
 *
 * <p>Recipes are RF-cost based: {@code energyrequired} is the total energy the machine spends to
 * complete the recipe.
 */
public class MaceratorRecipeSerializer {

    public static final MapCodec<MaceratorRecipeWrapper> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(MaceratorRecipeWrapper::ingredient),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(MaceratorRecipeWrapper::result),
        Codec.INT.fieldOf("energyrequired").forGetter(MaceratorRecipeWrapper::energyRequired),
        Codec.FLOAT.optionalFieldOf("experience", MaceratorRecipeWrapper.DEFAULT_EXPERIENCE).forGetter(MaceratorRecipeWrapper::experience)
    ).apply(i, MaceratorRecipeWrapper::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipeWrapper> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MaceratorRecipeWrapper::ingredient,
            ItemStackTemplate.STREAM_CODEC, MaceratorRecipeWrapper::result,
            ByteBufCodecs.VAR_INT, MaceratorRecipeWrapper::energyRequired,
            ByteBufCodecs.FLOAT, MaceratorRecipeWrapper::experience,
            MaceratorRecipeWrapper::new
        );

    public static final RecipeSerializer<MaceratorRecipeWrapper> INSTANCE =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private MaceratorRecipeSerializer() {}
}
