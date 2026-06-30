package com.logistics.automation.alloysmelter;

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
 * The JSON/network representation of an alloy-smelter recipe's chance-based byproduct (item + chance).
 * The resolver maps it to the framework's {@link com.logistics.core.machine.component.ChanceOutput},
 * which owns the rolling logic. Optional on a recipe — only ore-processing recipes carry one (slag).
 */
public record AlloySmelterByproduct(Item item, float chance) {

    public AlloySmelterByproduct {
        // Mirror ChanceOutput's contract: finite and non-negative (>1 is allowed — guaranteed + bonus).
        if (!Float.isFinite(chance) || chance < 0f) {
            throw new IllegalArgumentException("chance must be finite and non-negative, got " + chance);
        }
    }

    public static final Codec<AlloySmelterByproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(AlloySmelterByproduct::item),
        Codec.FLOAT.fieldOf("chance").forGetter(AlloySmelterByproduct::chance)
    ).apply(i, AlloySmelterByproduct::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmelterByproduct> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.ITEM), AlloySmelterByproduct::item,
        ByteBufCodecs.FLOAT, AlloySmelterByproduct::chance,
        AlloySmelterByproduct::new
    );

    public ItemStack stack(int count) {
        return new ItemStack(item, count);
    }
}
