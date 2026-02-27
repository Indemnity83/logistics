package com.logistics.core.fabricator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Factory for the kiln recipe serializer.
 * This is registered with Minecraft's recipe system to prevent parsing errors.
 * Actual recipe loading and matching is handled by KilnRecipeManager.
 *
 * <p>Note: In MC 26.1+, RecipeSerializer is a record rather than an interface,
 * so this class creates an instance rather than implementing the type.
 * TODO: Implement a real recipe serializer
 */
public class KilnRecipeSerializer {

    public static RecipeSerializer<KilnRecipeWrapper> create() {
        MapCodec<KilnRecipeWrapper> codec = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("type", "").forGetter(recipe -> "logistics:kiln")
            ).apply(instance, type -> new KilnRecipeWrapper())
        );
        StreamCodec<RegistryFriendlyByteBuf, KilnRecipeWrapper> streamCodec = StreamCodec.of(
            (buf, recipe) -> {},
            buf -> new KilnRecipeWrapper()
        );
        return new RecipeSerializer<>(codec, streamCodec);
    }
}
