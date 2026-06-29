package com.logistics.automation.macerator;

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
 * The JSON/network representation of a macerator recipe's chance-based byproduct (item + chance). The
 * resolver maps it to the framework's {@link com.logistics.core.machine.component.ChanceOutput}, which
 * owns the rolling logic. Optional on a recipe — only ore→dust recipes carry one.
 */
public record MaceratorByproduct(Item item, float chance) {

    public static final Codec<MaceratorByproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(MaceratorByproduct::item),
        Codec.FLOAT.fieldOf("chance").forGetter(MaceratorByproduct::chance)
    ).apply(i, MaceratorByproduct::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaceratorByproduct> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.ITEM), MaceratorByproduct::item,
        ByteBufCodecs.FLOAT, MaceratorByproduct::chance,
        MaceratorByproduct::new
    );

    public ItemStack stack(int count) {
        return new ItemStack(item, count);
    }
}
