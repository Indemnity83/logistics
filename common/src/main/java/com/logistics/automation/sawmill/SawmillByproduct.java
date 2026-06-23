package com.logistics.automation.sawmill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A chance-based recipe byproduct. The chance doubles as the count: {@code floor(chance)} is always
 * produced and the fractional remainder is the probability of one extra — so 1.0 yields exactly one,
 * and 1.25 yields one guaranteed plus a 25% chance of a second (TE-style secondary outputs).
 */
public record SawmillByproduct(Item item, float chance) {

    public static final Codec<SawmillByproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(SawmillByproduct::item),
        Codec.FLOAT.fieldOf("chance").forGetter(SawmillByproduct::chance)
    ).apply(i, SawmillByproduct::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SawmillByproduct> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.ITEM), SawmillByproduct::item,
        ByteBufCodecs.FLOAT, SawmillByproduct::chance,
        SawmillByproduct::new
    );

    /** The amount always produced (the integer part of the chance). */
    public int guaranteedCount() {
        return (int) chance;
    }

    /** Rolls this cut's yield: the guaranteed count plus the fractional bonus. */
    public int roll(RandomSource random) {
        int guaranteed = (int) chance;
        float bonus = chance - guaranteed;
        return guaranteed + (bonus > 0f && random.nextFloat() < bonus ? 1 : 0);
    }

    public ItemStack stack(int count) {
        return new ItemStack(item, count);
    }
}
