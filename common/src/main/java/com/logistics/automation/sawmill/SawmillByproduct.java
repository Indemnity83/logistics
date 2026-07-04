package com.logistics.automation.sawmill;

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
 * The JSON/network representation of a sawmill recipe's chance-based byproduct (item + chance). The
 * resolver maps it to the framework's {@link com.logistics.core.machine.component.ChanceOutput},
 * which owns the rolling logic.
 */
public record SawmillByproduct(Item item, float chance) {

    public SawmillByproduct {
        // Mirror ChanceOutput's contract: finite and non-negative (>1 is allowed — guaranteed + bonus).
        if (!Float.isFinite(chance) || chance < 0f) {
            throw new IllegalArgumentException("chance must be finite and non-negative, got " + chance);
        }
    }

    public static final Codec<SawmillByproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(SawmillByproduct::item),
        Codec.FLOAT.fieldOf("chance").forGetter(SawmillByproduct::chance)
    ).apply(i, SawmillByproduct::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SawmillByproduct> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.ITEM), SawmillByproduct::item,
        ByteBufCodecs.FLOAT, SawmillByproduct::chance,
        SawmillByproduct::new
    );

    public ItemStack stack(int count) {
        return new ItemStack(item, count);
    }
}
