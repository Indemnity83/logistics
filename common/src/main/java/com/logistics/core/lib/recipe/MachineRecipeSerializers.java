package com.logistics.core.lib.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Builds a {@link RecipeSerializer} from its codecs, hiding the per-version shape difference: 26.x
 * exposes a concrete {@code RecipeSerializer} class, while 1.21.x makes it an interface to implement.
 * Machine serializers call {@link #create} and stay byte-identical across branches.
 *
 * <p>Mirrors the {@code ResourceId} / {@code NbtCompat} compat-shim pattern: this file is the single
 * per-version implementation; the machine serializers above it are shared.
 */
public final class MachineRecipeSerializers {

    private MachineRecipeSerializers() {}

    public static <R extends Recipe<?>> RecipeSerializer<R> create(
            MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<R> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
                return streamCodec;
            }
        };
    }
}
