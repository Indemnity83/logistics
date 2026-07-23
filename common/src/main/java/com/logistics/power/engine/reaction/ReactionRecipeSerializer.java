package com.logistics.power.engine.reaction;

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
 * Serializer for the Reaction Engine recipe. JSON shape:
 * <pre>
 * {
 *   "type": "logistics:reaction",
 *   "reactant": { "fluid": "logistics:core/liquid_ender", "amount": 250 },
 *   "reagent": "minecraft:echo_shard",
 *   "reagent_count": 1,
 *   "energy": 100000,
 *   "time": 500
 * }
 * </pre>
 * {@code reagent} accepts an item id, a tag ({@code "#..."}), or an ingredient object; {@code reagent_count}
 * defaults to 1. RF/t is not stored — it is derived as {@code energy / time}.
 */
public class ReactionRecipeSerializer {

    public static final MapCodec<ReactionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidResult.CODEC.fieldOf("reactant").forGetter(ReactionRecipe::reactant),
            Ingredient.CODEC.fieldOf("reagent").forGetter(ReactionRecipe::reagent),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("reagent_count", 1)
                    .forGetter(ReactionRecipe::reagentCount),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(ReactionRecipe::energy),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("time").forGetter(ReactionRecipe::time)
    ).apply(i, ReactionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReactionRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidResult.STREAM_CODEC, ReactionRecipe::reactant,
                    Ingredient.CONTENTS_STREAM_CODEC, ReactionRecipe::reagent,
                    ByteBufCodecs.VAR_INT, ReactionRecipe::reagentCount,
                    ByteBufCodecs.VAR_INT, ReactionRecipe::energy,
                    ByteBufCodecs.VAR_INT, ReactionRecipe::time,
                    ReactionRecipe::new);

    public static final RecipeSerializer<ReactionRecipe> INSTANCE =
            MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private ReactionRecipeSerializer() {}
}
