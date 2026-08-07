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
 * Recipe serializer for transposer recipes (item + signed fluid → optional item). {@code energy} is the
 * total RF the machine spends; {@code fluid} is a {@link SignedFluidAmount} ({@code {"fluid": id,
 * "amount": <signed mB>}}) — negative drains the tank, positive fills it. {@code result} is omitted for
 * a recipe that only consumes its input (e.g. cactus → water).
 */
public class TransposerRecipeSerializer {

    public static final MapCodec<TransposerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("input").forGetter(TransposerRecipe::input),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", TransposerRecipe.DEFAULT_INGREDIENT_COUNT).forGetter(TransposerRecipe::inputCount),
        ItemResult.CODEC.optionalFieldOf("result").forGetter(TransposerRecipe::result),
        SignedFluidAmount.CODEC.fieldOf("fluid").forGetter(TransposerRecipe::fluid),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(TransposerRecipe::energy),
        Codec.FLOAT.optionalFieldOf("experience", TransposerRecipe.DEFAULT_EXPERIENCE).forGetter(TransposerRecipe::experience),
        RecipeByproduct.CODEC.optionalFieldOf("byproduct").forGetter(TransposerRecipe::byproduct)
    ).apply(i, TransposerRecipe::new));

    // 1.21.1's StreamCodec.composite caps at 6 component pairs, so input+count collapse into an
    // InputCount pair (same technique as the alloy smelter's CountedIngredient) to fit the arity.
    private record InputCount(Ingredient input, int count) {}

    private static final StreamCodec<RegistryFriendlyByteBuf, InputCount> INPUT_COUNT_STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, InputCount::input,
            ByteBufCodecs.VAR_INT, InputCount::count,
            InputCount::new
        );

    public static final StreamCodec<RegistryFriendlyByteBuf, TransposerRecipe> STREAM_CODEC =
        StreamCodec.composite(
            INPUT_COUNT_STREAM_CODEC, r -> new InputCount(r.input(), r.inputCount()),
            ByteBufCodecs.optional(ItemResult.STREAM_CODEC), TransposerRecipe::result,
            SignedFluidAmount.STREAM_CODEC, TransposerRecipe::fluid,
            ByteBufCodecs.VAR_INT, TransposerRecipe::energy,
            ByteBufCodecs.FLOAT, TransposerRecipe::experience,
            ByteBufCodecs.optional(RecipeByproduct.STREAM_CODEC), TransposerRecipe::byproduct,
            (inputCount, result, fluid, energy, experience, byproduct) -> new TransposerRecipe(
                inputCount.input(), inputCount.count(), result, fluid, energy, experience, byproduct)
        );

    public static final RecipeSerializer<TransposerRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private TransposerRecipeSerializer() {}
}
