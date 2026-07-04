package com.logistics.automation.macerator;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MaceratorRecipeWrapper")
class MaceratorRecipeTest extends MinecraftTestEnvironment {

    private static MaceratorRecipeWrapper rawIronRecipe() {
        return new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            1,
            ItemResult.of(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_ENERGY,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE,
            Optional.empty()
        );
    }

    // ==================== matches(ItemStack) ====================

    @Test
    @DisplayName("should match when ingredient matches input stack")
    void matchesCorrectItem() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.RAW_IRON))).isTrue();
    }

    @Test
    @DisplayName("should not match when ingredient does not match input stack")
    void doesNotMatchWrongItem() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.COAL))).isFalse();
    }

    @Test
    @DisplayName("should not match empty stack")
    void doesNotMatchEmptyStack() {
        assertThat(rawIronRecipe().matches(ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("should match stack with count greater than 1")
    void matchesStackWithMultipleItems() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.RAW_IRON, 5))).isTrue();
    }

    // ==================== getters ====================

    @Test
    @DisplayName("should return correct result item count")
    void resultItemCount() {
        ItemStack result = rawIronRecipe().getResultItem();
        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.is(Items.IRON_INGOT)).isTrue();
    }

    @Test
    @DisplayName("should preserve non-default energy required")
    void energy() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            1,
            ItemResult.of(Items.IRON_INGOT, 2),
            500,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE,
            Optional.empty()
        );
        assertThat(recipe.energy()).isEqualTo(500);
    }

    @Test
    @DisplayName("should reject non-positive energy required")
    void rejectsNonPositiveEnergyRequired() {
        for (int energy : new int[] {0, -1}) {
            assertThatThrownBy(() -> new MaceratorRecipeWrapper(
                    Ingredient.of(Items.RAW_IRON),
                    1,
                    ItemResult.of(Items.IRON_INGOT, 2),
                    energy,
                    MaceratorRecipeWrapper.DEFAULT_EXPERIENCE,
                    Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            1,
            ItemResult.of(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_ENERGY,
            0.7f,
            Optional.empty()
        );
        assertThat(recipe.experience()).isEqualTo(0.7f);
    }

    @Test
    @DisplayName("should reject a non-finite or negative experience")
    void rejectsNonFiniteOrNegativeExperience() {
        for (float experience : new float[] {-0.1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new MaceratorRecipeWrapper(
                    Ingredient.of(Items.RAW_IRON),
                    1,
                    ItemResult.of(Items.IRON_INGOT, 2),
                    MaceratorRecipeWrapper.DEFAULT_ENERGY,
                    experience,
                    Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("ingredient count: defaults to 1, and matching requires at least that many")
    void ingredientCount() {
        MaceratorRecipeWrapper twoSlabs = new MaceratorRecipeWrapper(
            Ingredient.of(Items.SMOOTH_STONE_SLAB),
            2,
            ItemResult.of(Items.IRON_INGOT, 1),
            MaceratorRecipeWrapper.DEFAULT_ENERGY,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE,
            Optional.empty()
        );
        assertThat(twoSlabs.ingredientCount()).isEqualTo(2);
        assertThat(rawIronRecipe().ingredientCount()).isEqualTo(1);

        // A 2-item recipe only matches when the input stack actually holds 2+.
        assertThat(twoSlabs.matches(new SingleRecipeInput(new ItemStack(Items.SMOOTH_STONE_SLAB, 1)), null)).isFalse();
        assertThat(twoSlabs.matches(new SingleRecipeInput(new ItemStack(Items.SMOOTH_STONE_SLAB, 2)), null)).isTrue();
    }

    // ==================== serializer codec round-trip ====================

    @Nested
    @DisplayName("MaceratorRecipeSerializer")
    class Serializer {

        private RegistryAccess registries;
        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips ingredient, result, energy required, and experience")
        void roundTripPreservesFields() {
            MaceratorRecipeWrapper original = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                1,
                ItemResult.of(Items.IRON_INGOT, 2),
                2000,
                0.7f,
                Optional.empty()
            );

            Tag encoded = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            MaceratorRecipeWrapper decoded = MaceratorRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();

            assertThat(decoded.matches(new ItemStack(Items.IRON_ORE))).isTrue();
            assertThat(decoded.matches(new ItemStack(Items.COAL))).isFalse();
            assertThat(decoded.getResultItem().is(Items.IRON_INGOT)).isTrue();
            assertThat(decoded.getResultItem().getCount()).isEqualTo(2);
            assertThat(decoded.energy()).isEqualTo(2000);
            assertThat(decoded.experience()).isEqualTo(0.7f);
            assertThat(decoded.byproduct()).isEmpty();
        }

        @Test
        @DisplayName("round-trips a chance byproduct, and omits it when absent")
        void roundTripPreservesByproduct() {
            MaceratorRecipeWrapper original = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                1,
                ItemResult.of(Items.IRON_INGOT, 2),
                2000,
                0.7f,
                Optional.of(new MaceratorByproduct(Items.GOLD_NUGGET, 0.1f))
            );

            Tag encoded = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            MaceratorRecipeWrapper decoded = MaceratorRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();

            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.GOLD_NUGGET);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.1f);

            // A recipe without a byproduct omits the field entirely.
            Tag plain = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, rawIronRecipe()).getOrThrow();
            assertThat(((CompoundTag) plain).contains("byproduct")).isFalse();
        }

        @Test
        @DisplayName("stream codec round-trips the byproduct (present and absent) for recipe sync")
        void streamRoundTripPreservesByproduct() {
            MaceratorRecipeWrapper withByproduct = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                1,
                ItemResult.of(Items.IRON_INGOT, 2),
                2000,
                0.7f,
                Optional.of(new MaceratorByproduct(Items.GOLD_NUGGET, 0.1f))
            );
            MaceratorRecipeWrapper decoded = streamRoundTrip(withByproduct);
            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.GOLD_NUGGET);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.1f);

            // The optional byproduct stays absent across the sync path.
            assertThat(streamRoundTrip(rawIronRecipe()).byproduct()).isEmpty();
        }

        private MaceratorRecipeWrapper streamRoundTrip(MaceratorRecipeWrapper recipe) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            MaceratorRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
            return MaceratorRecipeSerializer.STREAM_CODEC.decode(buf);
        }

        @Test
        @DisplayName("omits experience when default and decodes it back to the default")
        void defaultExperienceIsOptional() {
            MaceratorRecipeWrapper original = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                1,
                ItemResult.of(Items.IRON_INGOT, 1),
                200,
                MaceratorRecipeWrapper.DEFAULT_EXPERIENCE,
                Optional.empty()
            );

            Tag encoded = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            assertThat(((CompoundTag) encoded).contains("experience")).isFalse();

            MaceratorRecipeWrapper decoded = MaceratorRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
            assertThat(decoded.experience()).isEqualTo(MaceratorRecipeWrapper.DEFAULT_EXPERIENCE);
        }
    }
}
