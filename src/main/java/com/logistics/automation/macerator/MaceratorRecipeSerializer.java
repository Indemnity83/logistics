package com.logistics.automation.macerator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer for macerator recipes.
 * Registered with Minecraft's recipe system to prevent parse errors.
 * Actual loading is handled by MaceratorRecipeManager.
 */
public class MaceratorRecipeSerializer implements RecipeSerializer<MaceratorRecipeWrapper> {

    @Override
    public MapCodec<MaceratorRecipeWrapper> codec() {
        return RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("type", "").forGetter(recipe -> "logistics:macerator")
            ).apply(instance, type -> new MaceratorRecipeWrapper())
        );
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipeWrapper> streamCodec() {
        return StreamCodec.of(
            (buf, recipe) -> {},
            buf -> new MaceratorRecipeWrapper()
        );
    }
}
