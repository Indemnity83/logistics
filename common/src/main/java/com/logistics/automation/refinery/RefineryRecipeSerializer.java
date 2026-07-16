package com.logistics.automation.refinery;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.logistics.core.lib.recipe.RecipeByproduct;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for refinery recipes (fluid → fluid + optional item byproduct). {@code energy} is
 * the total RF the machine spends; {@code input} and {@code result} are {@link FluidResult}s
 * ({@code {"fluid": id, "amount": <mB>}}).
 */
public class RefineryRecipeSerializer {

    public static final MapCodec<RefineryRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        FluidResult.CODEC.fieldOf("input").forGetter(RefineryRecipe::input),
        FluidResult.CODEC.fieldOf("result").forGetter(RefineryRecipe::result),
        RecipeByproduct.CODEC.optionalFieldOf("byproduct").forGetter(RefineryRecipe::byproduct),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(RefineryRecipe::energy),
        Codec.FLOAT.optionalFieldOf("experience", RefineryRecipe.DEFAULT_EXPERIENCE).forGetter(RefineryRecipe::experience)
    ).apply(i, RefineryRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> STREAM_CODEC =
        StreamCodec.composite(
            FluidResult.STREAM_CODEC, RefineryRecipe::input,
            FluidResult.STREAM_CODEC, RefineryRecipe::result,
            ByteBufCodecs.optional(RecipeByproduct.STREAM_CODEC), RefineryRecipe::byproduct,
            ByteBufCodecs.VAR_INT, RefineryRecipe::energy,
            ByteBufCodecs.FLOAT, RefineryRecipe::experience,
            RefineryRecipe::new
        );

    public static final RecipeSerializer<RefineryRecipe> INSTANCE =
        MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private RefineryRecipeSerializer() {}
}
