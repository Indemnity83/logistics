package com.logistics.pipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/**
 * Domain data structures for pipe components.
 */
public final class PipeDataComponents {
    private PipeDataComponents() {}

    /**
     * Contents of a fluid packet — the internal, hidden item used to move a fluid through the item
     * logistics network. {@code quantumMb} is baked in at creation so packets minted under different
     * configured quanta stay distinct network keys and keep their original volume forever.
     */
    public record FluidPacket(Fluid fluid, long quantumMb) {
        public static final Codec<FluidPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidPacket::fluid),
                        Codec.LONG.fieldOf("quantum_mb").forGetter(FluidPacket::quantumMb))
                .apply(instance, FluidPacket::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), FluidPacket::fluid,
                ByteBufCodecs.VAR_LONG, FluidPacket::quantumMb,
                FluidPacket::new);
    }

    /**
     * Immutable record storing weathering state for copper pipes.
     */
    public record WeatheringState(int oxidationStage, boolean waxed) {
        public static final Codec<WeatheringState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("oxidation_stage").forGetter(WeatheringState::oxidationStage),
                        Codec.BOOL.optionalFieldOf("waxed", false).forGetter(WeatheringState::waxed))
                .apply(instance, WeatheringState::new));

        public static final WeatheringState DEFAULT = new WeatheringState(0, false);

        public boolean isDefault() {
            return oxidationStage == 0 && !waxed;
        }
    }
}
