package com.logistics.automation.sawmill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.recipe.ItemResult;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SawmillRecipe")
class SawmillRecipeTest extends MinecraftTestEnvironment {

    private static SawmillRecipe oakLogRecipe() {
        return new SawmillRecipe(
                Ingredient.of(Items.OAK_LOG),
                1,
                ItemResult.of(Items.OAK_PLANKS, 6),
                Optional.empty(),
                2000,
                SawmillRecipe.DEFAULT_EXPERIENCE);
    }

    // ==================== matches ====================

    @Test
    @DisplayName("should match when ingredient matches input stack")
    void matchesCorrectItem() {
        assertThat(oakLogRecipe().matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG)), null)).isTrue();
    }

    @Test
    @DisplayName("should not match when ingredient does not match input stack")
    void doesNotMatchWrongItem() {
        assertThat(oakLogRecipe().matches(new SingleRecipeInput(new ItemStack(Items.COAL)), null)).isFalse();
    }

    @Test
    @DisplayName("should not match empty stack")
    void doesNotMatchEmptyStack() {
        assertThat(oakLogRecipe().matches(new SingleRecipeInput(ItemStack.EMPTY), null)).isFalse();
    }

    @Test
    @DisplayName("ingredient count: defaults to 1, and matching requires at least that many")
    void ingredientCount() {
        SawmillRecipe twoLogs = new SawmillRecipe(
                Ingredient.of(Items.OAK_LOG),
                2,
                ItemResult.of(Items.OAK_PLANKS, 12),
                Optional.empty(),
                2000,
                SawmillRecipe.DEFAULT_EXPERIENCE);

        assertThat(twoLogs.ingredientCount()).isEqualTo(2);
        assertThat(oakLogRecipe().ingredientCount()).isEqualTo(1);

        assertThat(twoLogs.matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG, 1)), null)).isFalse();
        assertThat(twoLogs.matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG, 2)), null)).isTrue();
    }

    // ==================== acceptsAsInput ====================

    @Test
    @DisplayName("acceptsAsInput: matches on ingredient alone, ignoring ingredientCount")
    void acceptsAsInputIgnoresCount() {
        SawmillRecipe eightKelp = new SawmillRecipe(
                Ingredient.of(Items.KELP),
                8,
                ItemResult.of(Items.OAK_PLANKS, 1),
                Optional.empty(),
                2000,
                SawmillRecipe.DEFAULT_EXPERIENCE);

        // A single delivered item never satisfies matches() against a count-8 recipe, but it must
        // still pass acceptsAsInput() or a hopper/pipe could never place kelp into the slot at all.
        assertThat(eightKelp.matches(new SingleRecipeInput(new ItemStack(Items.KELP, 1)), null)).isFalse();
        assertThat(eightKelp.acceptsAsInput(new ItemStack(Items.KELP, 1))).isTrue();
        assertThat(eightKelp.acceptsAsInput(new ItemStack(Items.COAL, 1))).isFalse();
    }

    // ==================== getters ====================

    @Test
    @DisplayName("should return correct result item count")
    void resultItemCount() {
        ItemStack result = oakLogRecipe().getResultItem();
        assertThat(result.getCount()).isEqualTo(6);
        assertThat(result.is(Items.OAK_PLANKS)).isTrue();
    }

    @Test
    @DisplayName("should preserve non-default energy")
    void energy() {
        assertThat(oakLogRecipe().energy()).isEqualTo(2000);
    }

    @Test
    @DisplayName("should reject non-positive energy")
    void rejectsNonPositiveEnergy() {
        for (int energy : new int[] {0, -1}) {
            assertThatThrownBy(() -> new SawmillRecipe(
                            Ingredient.of(Items.OAK_LOG), 1, ItemResult.of(Items.OAK_PLANKS, 6), Optional.empty(),
                            energy, SawmillRecipe.DEFAULT_EXPERIENCE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        SawmillRecipe recipe = new SawmillRecipe(
                Ingredient.of(Items.OAK_LOG), 1, ItemResult.of(Items.OAK_PLANKS, 6), Optional.empty(), 2000, 0.7f);
        assertThat(recipe.experience()).isEqualTo(0.7f);
    }

    @Test
    @DisplayName("should reject a non-finite or negative experience")
    void rejectsNonFiniteOrNegativeExperience() {
        for (float experience : new float[] {-0.1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new SawmillRecipe(
                            Ingredient.of(Items.OAK_LOG), 1, ItemResult.of(Items.OAK_PLANKS, 6), Optional.empty(),
                            2000, experience))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== serializer codec round-trip ====================

    @Nested
    @DisplayName("SawmillRecipeSerializer")
    class Serializer {

        private RegistryAccess registries;
        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips ingredient, count, result, and energy")
        void roundTripPreservesFields() {
            SawmillRecipe decoded = roundTrip(oakLogRecipe());

            assertThat(decoded.matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG)), null)).isTrue();
            assertThat(decoded.matches(new SingleRecipeInput(new ItemStack(Items.COAL)), null)).isFalse();
            assertThat(decoded.getResultItem().is(Items.OAK_PLANKS)).isTrue();
            assertThat(decoded.getResultItem().getCount()).isEqualTo(6);
            assertThat(decoded.energy()).isEqualTo(2000);
            assertThat(decoded.byproduct()).isEmpty();
        }

        @Test
        @DisplayName("round-trips a non-default experience, and omits it when default")
        void roundTripPreservesExperience() {
            SawmillRecipe original = new SawmillRecipe(
                    Ingredient.of(Items.OAK_LOG), 1, ItemResult.of(Items.OAK_PLANKS, 6), Optional.empty(), 2000, 0.7f);

            assertThat(roundTrip(original).experience()).isEqualTo(0.7f);

            Tag plain = SawmillRecipeSerializer.CODEC.codec().encodeStart(ops, oakLogRecipe()).getOrThrow();
            assertThat(((CompoundTag) plain).contains("experience")).isFalse();
        }

        @Test
        @DisplayName("round-trips a chance byproduct, and omits it when absent")
        void roundTripPreservesByproduct() {
            SawmillRecipe original = new SawmillRecipe(
                    Ingredient.of(Items.OAK_LOG),
                    1,
                    ItemResult.of(Items.OAK_PLANKS, 6),
                    Optional.of(new RecipeByproduct(Items.STICK, 0.5f)),
                    2000,
                    SawmillRecipe.DEFAULT_EXPERIENCE);

            SawmillRecipe decoded = roundTrip(original);
            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.STICK);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.5f);

            Tag plain = SawmillRecipeSerializer.CODEC.codec().encodeStart(ops, oakLogRecipe()).getOrThrow();
            assertThat(((CompoundTag) plain).contains("byproduct")).isFalse();
        }

        @Test
        @DisplayName("stream codec round-trips the byproduct for recipe sync")
        void streamRoundTripPreservesByproduct() {
            SawmillRecipe withByproduct = new SawmillRecipe(
                    Ingredient.of(Items.OAK_LOG),
                    1,
                    ItemResult.of(Items.OAK_PLANKS, 6),
                    Optional.of(new RecipeByproduct(Items.STICK, 0.5f)),
                    2000,
                    SawmillRecipe.DEFAULT_EXPERIENCE);

            SawmillRecipe decoded = streamRoundTrip(withByproduct);
            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.STICK);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.5f);

            assertThat(streamRoundTrip(oakLogRecipe()).byproduct()).isEmpty();
        }

        private SawmillRecipe roundTrip(SawmillRecipe recipe) {
            Tag encoded = SawmillRecipeSerializer.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
            return SawmillRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
        }

        private SawmillRecipe streamRoundTrip(SawmillRecipe recipe) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            SawmillRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
            return SawmillRecipeSerializer.STREAM_CODEC.decode(buf);
        }
    }
}
