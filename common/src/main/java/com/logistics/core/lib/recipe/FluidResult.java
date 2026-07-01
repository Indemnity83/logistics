package com.logistics.core.lib.recipe;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/**
 * Version-agnostic fluid output of a custom-recipe machine: a fluid plus an author-facing millibucket
 * amount. The mB amount is converted to platform-native fluid units via {@link FluidUnits} only when it
 * is deposited, so recipes stay loader-independent. Analog to {@link MachineResult} for the item result.
 */
public record FluidResult(Fluid fluid, int millibuckets) {

    /** Codec for a recipe fluid output ({@code {"fluid": "namespace:id", "amount": <mB>}}). */
    public static final Codec<FluidResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidResult::fluid),
                    Codec.INT.fieldOf("amount").forGetter(FluidResult::millibuckets))
            .apply(instance, FluidResult::new));

    /** Network codec for syncing the fluid result to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), FluidResult::fluid,
            ByteBufCodecs.INT, FluidResult::millibuckets,
            FluidResult::new);

    /** A fluid-identity key for the storage layer. */
    public IFluidKey key() {
        return SimpleFluidKey.of(fluid);
    }

    /** The amount in platform-native fluid units (mB × loader factor). */
    public long nativeAmount() {
        return FluidUnits.mb(millibuckets);
    }
}
