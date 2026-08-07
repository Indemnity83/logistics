package com.logistics.pipe.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the structural invariant that a {@link FluidPacket} can never carry a non-positive
 * {@code amountMb} — enforced at every construction path: the compact constructor (in-JVM callers,
 * including both codecs below since they both ultimately call {@code FluidPacket::new}), and
 * additionally the NBT/JSON {@link FluidPacket#CODEC}'s {@code validate()} wrapper (graceful
 * {@link com.mojang.serialization.DataResult#error} on decode, rather than an uncaught exception during
 * world load).
 */
@DisplayName("FluidPacket invariants")
class PipeDataComponentsTest extends MinecraftTestEnvironment {

    private RegistryAccess registries;
    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    // ==================== construction ====================

    @Test
    @DisplayName("rejects a zero or negative amountMb at construction")
    void rejectsNonPositiveAtConstruction() {
        for (long amount : new long[] {0, -1, -1000}) {
            assertThatThrownBy(() -> new FluidPacket(Fluids.WATER, amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejects a null fluid at construction")
    void rejectsNullFluidAtConstruction() {
        assertThatThrownBy(() -> new FluidPacket(null, 250)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("accepts any positive amountMb (positive control)")
    void acceptsPositiveAmount() {
        FluidPacket packet = new FluidPacket(Fluids.WATER, 1);
        assertThat(packet.amountMb()).isEqualTo(1);

        packet = new FluidPacket(Fluids.LAVA, Long.MAX_VALUE);
        assertThat(packet.amountMb()).isEqualTo(Long.MAX_VALUE);
    }

    // ==================== CODEC (NBT/JSON) ====================

    @Test
    @DisplayName("codec round-trips the fluid and amount")
    void codecRoundTrip() {
        FluidPacket original = new FluidPacket(Fluids.LAVA, 500);

        Tag encoded = FluidPacket.CODEC.encodeStart(ops, original).getOrThrow();
        FluidPacket decoded = FluidPacket.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.fluid()).isEqualTo(Fluids.LAVA);
        assertThat(decoded.amountMb()).isEqualTo(500);
    }

    @Test
    @DisplayName("codec decode rejects a zero or negative amountMb, gracefully")
    void codecDecodeRejectsNonPositiveAmount() {
        for (long amount : new long[] {0, -1}) {
            // Build the encoded form by hand (a real FluidPacket can never hold this value), simulating
            // a corrupted/hand-edited save.
            FluidPacket valid = new FluidPacket(Fluids.WATER, 1);
            Tag validEncoded = FluidPacket.CODEC.encodeStart(ops, valid).getOrThrow();
            Tag corrupted = ((net.minecraft.nbt.CompoundTag) validEncoded).copy();
            ((net.minecraft.nbt.CompoundTag) corrupted).putLong("amount_mb", amount);

            assertThat(FluidPacket.CODEC.parse(ops, corrupted).isError())
                    .as("amount_mb=%d should be rejected", amount)
                    .isTrue();
        }
    }

    // ==================== STREAM_CODEC (network) ====================

    @Test
    @DisplayName("stream codec round-trips the fluid and amount")
    void streamCodecRoundTrip() {
        FluidPacket original = new FluidPacket(Fluids.LAVA, 500);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        FluidPacket.STREAM_CODEC.encode(buf, original);
        FluidPacket decoded = FluidPacket.STREAM_CODEC.decode(buf);

        assertThat(decoded.fluid()).isEqualTo(Fluids.LAVA);
        assertThat(decoded.amountMb()).isEqualTo(500);
    }

    @Test
    @DisplayName("stream codec cannot decode a non-positive amountMb (throws via the shared constructor)")
    void streamCodecDecodeRejectsNonPositiveAmount() {
        // FluidPacket.STREAM_CODEC has no DataResult error layer (unlike CODEC) — an invalid value
        // reaching the shared compact constructor throws directly out of decode. Write a malformed wire
        // payload by hand (a real FluidPacket can never encode this) to prove the constructor still
        // catches it rather than silently producing an invalid instance.
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        ByteBufCodecs.registry(Registries.FLUID).encode(buf, Fluids.WATER);
        ByteBufCodecs.VAR_LONG.encode(buf, 0L);

        assertThatThrownBy(() -> FluidPacket.STREAM_CODEC.decode(buf))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
