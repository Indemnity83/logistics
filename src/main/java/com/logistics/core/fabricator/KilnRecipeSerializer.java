package com.logistics.core.fabricator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for kiln recipes.
 * This is registered with Minecraft's recipe system to prevent parsing errors.
 * It returns dummy wrapper recipes that satisfy Minecraft, but actual recipe
 * loading and matching is handled by KilnRecipeManager.
 * TODO: Implement a real recipe serializer
 */
public class KilnRecipeSerializer implements RecipeSerializer<KilnRecipeWrapper> {

    @Override
    public MapCodec<KilnRecipeWrapper> codec() {
        // Return a minimal codec that accepts any JSON structure
        // We don't validate or use the data - just let Minecraft think it loaded successfully
        return RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("type", "").forGetter(recipe -> "logistics:kiln")
            ).apply(instance, type -> new KilnRecipeWrapper())
        );
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KilnRecipeWrapper> streamCodec() {
        // Return a minimal stream codec - we don't actually sync these recipes to clients
        return StreamCodec.of(
            (buf, recipe) -> {}, // Write nothing
            buf -> new KilnRecipeWrapper() // Read nothing, return dummy
        );
    }
}
