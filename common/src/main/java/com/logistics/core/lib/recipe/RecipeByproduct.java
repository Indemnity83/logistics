package com.logistics.core.lib.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The JSON/network representation of a machine recipe's chance-based byproduct (item + chance). Shared
 * across machine domains alongside {@link ItemResult} / {@link FluidResult}. A recipe's resolver maps it
 * to the framework's {@link com.logistics.core.machine.component.ChanceOutput}, which owns the rolling
 * logic. Optional on a recipe — only recipes with a bonus output (e.g. ore→dust slag) carry one.
 */
public record RecipeByproduct(Item item, float chance) {

    public RecipeByproduct {
        // Mirror ChanceOutput's contract: finite and non-negative (>1 is allowed — guaranteed + bonus).
        if (!Float.isFinite(chance) || chance < 0f) {
            throw new IllegalArgumentException("chance must be finite and non-negative, got " + chance);
        }
    }

    public static final Codec<RecipeByproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(RecipeByproduct::item),
        Codec.FLOAT.fieldOf("chance").forGetter(RecipeByproduct::chance)
    ).apply(i, RecipeByproduct::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeByproduct> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.ITEM), RecipeByproduct::item,
        ByteBufCodecs.FLOAT, RecipeByproduct::chance,
        RecipeByproduct::new
    );

    public ItemStack stack(int count) {
        return new ItemStack(item, count);
    }
}
