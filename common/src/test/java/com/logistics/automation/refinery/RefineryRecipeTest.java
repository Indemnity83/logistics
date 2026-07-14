package com.logistics.automation.refinery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.lib.recipe.RecipeByproduct;
import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefineryRecipe")
class RefineryRecipeTest extends MinecraftTestEnvironment {

    private static RefineryRecipe fuelOilRecipe() {
        return new RefineryRecipe(
                new FluidResult(Fluids.WATER, 200),
                new FluidResult(Fluids.LAVA, 150),
                Optional.of(new RecipeByproduct(Items.GUNPOWDER, 0.5f)),
                5000,
                RefineryRecipe.DEFAULT_EXPERIENCE);
    }

    private static RefineryRecipe bioFuelRecipe() {
        return new RefineryRecipe(
                new FluidResult(Fluids.WATER, 200),
                new FluidResult(Fluids.LAVA, 100),
                Optional.empty(),
                5000,
                RefineryRecipe.DEFAULT_EXPERIENCE);
    }

    // ==================== getters ====================

    @Test
    @DisplayName("exposes input, result, byproduct, and energy")
    void getters() {
        RefineryRecipe recipe = fuelOilRecipe();
        assertThat(recipe.input().fluid()).isEqualTo(Fluids.WATER);
        assertThat(recipe.input().millibuckets()).isEqualTo(200);
        assertThat(recipe.result().fluid()).isEqualTo(Fluids.LAVA);
        assertThat(recipe.result().millibuckets()).isEqualTo(150);
        assertThat(recipe.byproduct()).isPresent();
        assertThat(recipe.byproduct().get().item()).isEqualTo(Items.GUNPOWDER);
        assertThat(recipe.byproduct().get().chance()).isEqualTo(0.5f);
        assertThat(recipe.energy()).isEqualTo(5000);
    }

    @Test
    @DisplayName("byproduct is optional")
    void byproductOptional() {
        assertThat(bioFuelRecipe().byproduct()).isEmpty();
    }

    @Test
    @DisplayName("rejects non-positive energy")
    void rejectsNonPositiveEnergy() {
        for (int energy : new int[] {0, -1}) {
            assertThatThrownBy(() -> new RefineryRecipe(
                            new FluidResult(Fluids.WATER, 200), new FluidResult(Fluids.LAVA, 100),
                            Optional.empty(), energy, 0f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("rejects a non-finite or negative experience")
    void rejectsBadExperience() {
        for (float experience : new float[] {-0.1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new RefineryRecipe(
                            new FluidResult(Fluids.WATER, 200), new FluidResult(Fluids.LAVA, 100),
                            Optional.empty(), 5000, experience))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== serializer codec round-trip ====================

    @Nested
    @DisplayName("RefineryRecipeSerializer")
    class Serializer {

        private RegistryAccess registries;
        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips input, result, byproduct, and energy")
        void roundTripPreservesFields() {
            RefineryRecipe decoded = roundTrip(fuelOilRecipe());

            assertThat(decoded.input().fluid()).isEqualTo(Fluids.WATER);
            assertThat(decoded.input().millibuckets()).isEqualTo(200);
            assertThat(decoded.result().fluid()).isEqualTo(Fluids.LAVA);
            assertThat(decoded.result().millibuckets()).isEqualTo(150);
            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.GUNPOWDER);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.5f);
            assertThat(decoded.energy()).isEqualTo(5000);
        }

        @Test
        @DisplayName("omits an absent byproduct and decodes it back to empty")
        void byproductOptionalRoundTrip() {
            Tag encoded = RefineryRecipeSerializer.CODEC.codec().encodeStart(ops, bioFuelRecipe()).getOrThrow();
            assertThat(((CompoundTag) encoded).contains("byproduct")).isFalse();

            RefineryRecipe decoded = RefineryRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
            assertThat(decoded.byproduct()).isEmpty();
        }

        @Test
        @DisplayName("stream codec round-trips input, result, and byproduct for recipe sync")
        void streamRoundTripPreservesFields() {
            RefineryRecipe decoded = streamRoundTrip(fuelOilRecipe());

            assertThat(decoded.input().millibuckets()).isEqualTo(200);
            assertThat(decoded.result().fluid()).isEqualTo(Fluids.LAVA);
            assertThat(decoded.result().millibuckets()).isEqualTo(150);
            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.5f);
        }

        private RefineryRecipe roundTrip(RefineryRecipe recipe) {
            Tag encoded = RefineryRecipeSerializer.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
            return RefineryRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
        }

        private RefineryRecipe streamRoundTrip(RefineryRecipe recipe) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            RefineryRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
            return RefineryRecipeSerializer.STREAM_CODEC.decode(buf);
        }
    }
}
