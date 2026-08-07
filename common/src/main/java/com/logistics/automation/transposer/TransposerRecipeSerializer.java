package com.logistics.automation.transposer;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.logistics.core.lib.recipe.RecipeByproduct;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for transposer recipes (item + signed fluid → item). {@code energy} is the total
 * RF the machine spends; {@code fluid} is a {@link SignedFluidAmount} ({@code {"fluid": id, "amount":
 * <signed mB>}}) — negative drains the tank, positive fills it.
 */
public class TransposerRecipeSerializer {

    public static final MapCodec<TransposerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("input").forGetter(TransposerRecipe::input),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", TransposerRecipe.DEFAULT_INGREDIENT_COUNT).forGetter(TransposerRecipe::inputCount),
        ItemResult.CODEC.fieldOf("result").forGetter(TransposerRecipe::result),
        SignedFluidAmount.CODEC.fieldOf("fluid").forGetter(TransposerRecipe::fluid),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(TransposerRecipe::energy),
        Codec.FLOAT.optionalFieldOf("experience", TransposerRecipe.DEFAULT_EXPERIENCE).forGetter(TransposerRecipe::experience),
        RecipeByproduct.CODEC.optionalFieldOf("byproduct").forGetter(TransposerRecipe::byproduct)
    ).apply(i, TransposerRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransposerRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, TransposerRecipe::input,
            ByteBufCodecs.VAR_INT, TransposerRecipe::inputCount,
            ItemResult.STREAM_CODEC, TransposerRecipe::result,
            SignedFluidAmount.STREAM_CODEC, TransposerRecipe::fluid,
            ByteBufCodecs.VAR_INT, TransposerRecipe::energy,
            ByteBufCodecs.FLOAT, TransposerRecipe::experience,
            ByteBufCodecs.optional(RecipeByproduct.STREAM_CODEC), TransposerRecipe::byproduct,
            TransposerRecipe::new
        );

    public static final RecipeSerializer<TransposerRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private TransposerRecipeSerializer() {}
}
