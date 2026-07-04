package com.logistics.core.lib.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidResult")
class FluidResultTest extends MinecraftTestEnvironment {

    // ==================== validation ====================

    @Test
    @DisplayName("rejects an empty fluid")
    void rejectsEmptyFluid() {
        assertThatThrownBy(() -> new FluidResult(Fluids.EMPTY, 250))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects a non-positive amount")
    void rejectsNonPositiveAmount() {
        for (int amount : new int[] {0, -1}) {
            assertThatThrownBy(() -> new FluidResult(Fluids.WATER, amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== getters ====================

    @Test
    @DisplayName("exposes the fluid, amount, key, and native amount")
    void getters() {
        FluidResult result = new FluidResult(Fluids.WATER, 250);

        assertThat(result.fluid()).isEqualTo(Fluids.WATER);
        assertThat(result.millibuckets()).isEqualTo(250);
        assertThat(result.key()).isEqualTo(SimpleFluidKey.of(Fluids.WATER));
        assertThat(result.nativeAmount()).isEqualTo(FluidUnits.mb(250));
    }

    // ==================== serialization ====================

    private RegistryAccess registries;
    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    @Test
    @DisplayName("codec round-trips the fluid and amount")
    void codecRoundTrip() {
        FluidResult original = new FluidResult(Fluids.LAVA, 500);

        Tag encoded = FluidResult.CODEC.encodeStart(ops, original).getOrThrow();
        FluidResult decoded = FluidResult.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.fluid()).isEqualTo(Fluids.LAVA);
        assertThat(decoded.millibuckets()).isEqualTo(500);
    }

    @Test
    @DisplayName("stream codec round-trips the fluid and amount for recipe sync")
    void streamCodecRoundTrip() {
        FluidResult original = new FluidResult(Fluids.LAVA, 500);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        FluidResult.STREAM_CODEC.encode(buf, original);
        FluidResult decoded = FluidResult.STREAM_CODEC.decode(buf);

        assertThat(decoded.fluid()).isEqualTo(Fluids.LAVA);
        assertThat(decoded.millibuckets()).isEqualTo(500);
    }
}
