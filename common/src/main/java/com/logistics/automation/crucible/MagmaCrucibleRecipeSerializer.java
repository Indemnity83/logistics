package com.logistics.automation.crucible;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for magma crucible recipes (item → fluid). {@code energyrequired} is the total RF the
 * machine spends; {@code result} is a {@link FluidResult} ({@code {"fluid": id, "amount": <mB>}}).
 */
public class MagmaCrucibleRecipeSerializer {

    public static final MapCodec<MagmaCrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(MagmaCrucibleRecipe::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", MagmaCrucibleRecipe.DEFAULT_INGREDIENT_COUNT).forGetter(MagmaCrucibleRecipe::ingredientCount),
        FluidResult.CODEC.fieldOf("result").forGetter(MagmaCrucibleRecipe::result),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energyrequired").forGetter(MagmaCrucibleRecipe::energyRequired),
        Codec.FLOAT.optionalFieldOf("experience", MagmaCrucibleRecipe.DEFAULT_EXPERIENCE).forGetter(MagmaCrucibleRecipe::experience)
    ).apply(i, MagmaCrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagmaCrucibleRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MagmaCrucibleRecipe::ingredient,
            ByteBufCodecs.VAR_INT, MagmaCrucibleRecipe::ingredientCount,
            FluidResult.STREAM_CODEC, MagmaCrucibleRecipe::result,
            ByteBufCodecs.VAR_INT, MagmaCrucibleRecipe::energyRequired,
            ByteBufCodecs.FLOAT, MagmaCrucibleRecipe::experience,
            MagmaCrucibleRecipe::new
        );

    public static final RecipeSerializer<MagmaCrucibleRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private MagmaCrucibleRecipeSerializer() {}
}
