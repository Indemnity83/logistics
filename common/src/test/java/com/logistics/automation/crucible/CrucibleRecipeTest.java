package com.logistics.automation.crucible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CrucibleRecipe")
class CrucibleRecipeTest extends MinecraftTestEnvironment {

    private static CrucibleRecipe bitumenRecipe() {
        return new CrucibleRecipe(
                Ingredient.of(Items.COAL),
                1,
                new FluidResult(Fluids.LAVA, 250),
                2000,
                CrucibleRecipe.DEFAULT_EXPERIENCE);
    }

    // ==================== matches ====================

    @Test
    @DisplayName("should match when ingredient matches input stack")
    void matchesCorrectItem() {
        assertThat(bitumenRecipe().matches(new ItemStack(Items.COAL))).isTrue();
        assertThat(bitumenRecipe().matches(new SingleRecipeInput(new ItemStack(Items.COAL)), null)).isTrue();
    }

    @Test
    @DisplayName("should not match when ingredient does not match input stack")
    void doesNotMatchWrongItem() {
        assertThat(bitumenRecipe().matches(new ItemStack(Items.IRON_INGOT))).isFalse();
        assertThat(bitumenRecipe().matches(new SingleRecipeInput(new ItemStack(Items.IRON_INGOT)), null)).isFalse();
    }

    @Test
    @DisplayName("should not match empty stack")
    void doesNotMatchEmptyStack() {
        assertThat(bitumenRecipe().matches(ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("ingredient count: defaults to 1, and matching requires at least that many")
    void ingredientCount() {
        CrucibleRecipe twoCoal = new CrucibleRecipe(
                Ingredient.of(Items.COAL),
                2,
                new FluidResult(Fluids.LAVA, 250),
                2000,
                CrucibleRecipe.DEFAULT_EXPERIENCE);

        assertThat(twoCoal.ingredientCount()).isEqualTo(2);
        assertThat(bitumenRecipe().ingredientCount()).isEqualTo(1);

        assertThat(twoCoal.matches(new SingleRecipeInput(new ItemStack(Items.COAL, 1)), null)).isFalse();
        assertThat(twoCoal.matches(new SingleRecipeInput(new ItemStack(Items.COAL, 2)), null)).isTrue();
    }

    // ==================== getters ====================

    @Test
    @DisplayName("should expose the fluid result and energy")
    void fluidResultAndEnergy() {
        CrucibleRecipe recipe = bitumenRecipe();
        assertThat(recipe.result().fluid()).isEqualTo(Fluids.LAVA);
        assertThat(recipe.result().millibuckets()).isEqualTo(250);
        assertThat(recipe.energy()).isEqualTo(2000);
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        CrucibleRecipe recipe = new CrucibleRecipe(
                Ingredient.of(Items.COAL), 1, new FluidResult(Fluids.LAVA, 250), 2000, 0.7f);
        assertThat(recipe.experience()).isEqualTo(0.7f);
    }

    @Test
    @DisplayName("should reject non-positive energy required")
    void rejectsNonPositiveEnergyRequired() {
        for (int energy : new int[] {0, -1}) {
            assertThatThrownBy(() -> new CrucibleRecipe(
                            Ingredient.of(Items.COAL), 1, new FluidResult(Fluids.LAVA, 250), energy, 0f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("should reject a negative experience")
    void rejectsNegativeExperience() {
        assertThatThrownBy(() -> new CrucibleRecipe(
                        Ingredient.of(Items.COAL), 1, new FluidResult(Fluids.LAVA, 250), 2000, -0.1f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== serializer codec round-trip ====================

    @Nested
    @DisplayName("CrucibleRecipeSerializer")
    class Serializer {

        private RegistryAccess registries;
        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips ingredient, count, fluid result, energy, and experience")
        void roundTripPreservesFields() {
            CrucibleRecipe original = new CrucibleRecipe(
                    Ingredient.of(Items.COAL), 1, new FluidResult(Fluids.LAVA, 250), 2000, 0.7f);

            CrucibleRecipe decoded = roundTrip(original);

            assertThat(decoded.matches(new ItemStack(Items.COAL))).isTrue();
            assertThat(decoded.matches(new ItemStack(Items.IRON_INGOT))).isFalse();
            assertThat(decoded.result().fluid()).isEqualTo(Fluids.LAVA);
            assertThat(decoded.result().millibuckets()).isEqualTo(250);
            assertThat(decoded.energy()).isEqualTo(2000);
            assertThat(decoded.experience()).isEqualTo(0.7f);
        }

        @Test
        @DisplayName("stream codec round-trips the fluid result and energy for recipe sync")
        void streamRoundTripPreservesFields() {
            CrucibleRecipe decoded = streamRoundTrip(bitumenRecipe());

            assertThat(decoded.result().fluid()).isEqualTo(Fluids.LAVA);
            assertThat(decoded.result().millibuckets()).isEqualTo(250);
            assertThat(decoded.energy()).isEqualTo(2000);
        }

        @Test
        @DisplayName("omits experience when default and decodes it back to the default")
        void defaultExperienceIsOptional() {
            Tag encoded = CrucibleRecipeSerializer.CODEC.codec().encodeStart(ops, bitumenRecipe()).getOrThrow();
            assertThat(((CompoundTag) encoded).contains("experience")).isFalse();

            CrucibleRecipe decoded = CrucibleRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
            assertThat(decoded.experience()).isEqualTo(CrucibleRecipe.DEFAULT_EXPERIENCE);
        }

        private CrucibleRecipe roundTrip(CrucibleRecipe recipe) {
            Tag encoded = CrucibleRecipeSerializer.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
            return CrucibleRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
        }

        private CrucibleRecipe streamRoundTrip(CrucibleRecipe recipe) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            CrucibleRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
            return CrucibleRecipeSerializer.STREAM_CODEC.decode(buf);
        }
    }
}
