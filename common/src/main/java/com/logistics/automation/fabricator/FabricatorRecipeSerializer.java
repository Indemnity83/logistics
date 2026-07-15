package com.logistics.automation.fabricator;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.recipe.MachineRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for fabricator recipes. {@code ingredients} is a list of {@link SizedIngredient};
 * {@code energy} (RF) is required.
 */
public class FabricatorRecipeSerializer {

    public static final MapCodec<FabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            SizedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(FabricatorRecipe::ingredients),
            ItemResult.CODEC.fieldOf("result").forGetter(FabricatorRecipe::result),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(FabricatorRecipe::energy)
    ).apply(i, FabricatorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), FabricatorRecipe::ingredients,
                    ItemResult.STREAM_CODEC, FabricatorRecipe::result,
                    ByteBufCodecs.VAR_INT, FabricatorRecipe::energy,
                    FabricatorRecipe::new);

    public static final RecipeSerializer<FabricatorRecipe> INSTANCE =
            MachineRecipeSerializers.create(CODEC, STREAM_CODEC);

    private FabricatorRecipeSerializer() {}
}
