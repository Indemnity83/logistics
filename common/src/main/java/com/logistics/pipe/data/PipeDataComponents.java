package com.logistics.pipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

/**
 * Domain data structures for pipe components.
 */
public final class PipeDataComponents {
    private PipeDataComponents() {}

    /**
     * Contents of a fluid packet — the internal, hidden item used to move a fluid through the item
     * logistics network. {@code amountMb} is the packet's real, physical payload (always {@code > 0});
     * network order/supply bookkeeping never uses this value as an identity key — it's carried purely
     * for transport and destination insertion (see {@code FluidResourceKey}, whose identity is the
     * fluid alone).
     *
     * <p>{@code amountMb > 0} is enforced at every construction path: the compact constructor throws
     * for any in-JVM caller, and {@link #CODEC} additionally wraps the field in {@code validate()} so a
     * corrupted/hand-edited save decodes to a graceful {@link DataResult#error} (treated as an absent
     * component, matching {@code FluidSupplierModule.onTransferToStorage}'s existing null-data guard)
     * rather than throwing during world load. {@link #STREAM_CODEC} has no such graceful layer — an
     * invalid value reaching the network wire throws directly out of packet decode via the shared
     * compact constructor, which is acceptable for a malformed/malicious network payload.
     */
    public record FluidPacket(Fluid fluid, long amountMb) {
        public FluidPacket {
            Objects.requireNonNull(fluid, "fluid");
            if (amountMb <= 0) {
                throw new IllegalArgumentException("amountMb must be positive, got: " + amountMb);
            }
        }

        public static final Codec<FluidPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidPacket::fluid),
                        Codec.LONG
                                .validate(mb -> mb <= 0
                                        ? DataResult.error(() -> "amountMb must be positive, was " + mb)
                                        : DataResult.success(mb))
                                .fieldOf("amount_mb")
                                .forGetter(FluidPacket::amountMb))
                .apply(instance, FluidPacket::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), FluidPacket::fluid,
                ByteBufCodecs.VAR_LONG, FluidPacket::amountMb,
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
