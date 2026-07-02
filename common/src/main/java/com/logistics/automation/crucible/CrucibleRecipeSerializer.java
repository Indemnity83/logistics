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
 * Recipe serializer for crucible recipes (item → fluid). {@code energyrequired} is the total RF the
 * machine spends; {@code result} is a {@link FluidResult} ({@code {"fluid": id, "amount": <mB>}}).
 */
public class CrucibleRecipeSerializer {

    public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(CrucibleRecipe::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", CrucibleRecipe.DEFAULT_INGREDIENT_COUNT).forGetter(CrucibleRecipe::ingredientCount),
        FluidResult.CODEC.fieldOf("result").forGetter(CrucibleRecipe::result),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energyrequired").forGetter(CrucibleRecipe::energyRequired),
        Codec.FLOAT.optionalFieldOf("experience", CrucibleRecipe.DEFAULT_EXPERIENCE).forGetter(CrucibleRecipe::experience)
    ).apply(i, CrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrucibleRecipe::ingredient,
            ByteBufCodecs.VAR_INT, CrucibleRecipe::ingredientCount,
            FluidResult.STREAM_CODEC, CrucibleRecipe::result,
            ByteBufCodecs.VAR_INT, CrucibleRecipe::energyRequired,
            ByteBufCodecs.FLOAT, CrucibleRecipe::experience,
            CrucibleRecipe::new
        );

    public static final RecipeSerializer<CrucibleRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private CrucibleRecipeSerializer() {}
}
