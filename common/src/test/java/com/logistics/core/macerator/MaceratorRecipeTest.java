package com.logistics.core.macerator;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaceratorRecipeWrapper")
class MaceratorRecipeTest extends MinecraftTestEnvironment {

    private static MaceratorRecipeWrapper rawIronRecipe() {
        return new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_GRINDING_TIME,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE
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
    @DisplayName("should preserve non-default grinding time")
    void grindingTime() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            5,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE
        );
        assertThat(recipe.grindingTime()).isEqualTo(5);
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_GRINDING_TIME,
            0.7f
        );
        assertThat(recipe.experience()).isEqualTo(0.7f);
    }

    // ==================== serializer codec round-trip ====================

    @Nested
    @DisplayName("MaceratorRecipeSerializer")
    class Serializer {

        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips ingredient, result, grinding time, and experience")
        void roundTripPreservesFields() {
            MaceratorRecipeWrapper original = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                new ItemStackTemplate(Items.IRON_INGOT, 2),
                200,
                0.7f
            );

            Tag encoded = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            MaceratorRecipeWrapper decoded = MaceratorRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();

            assertThat(decoded.matches(new ItemStack(Items.IRON_ORE))).isTrue();
            assertThat(decoded.matches(new ItemStack(Items.COAL))).isFalse();
            assertThat(decoded.getResultItem().is(Items.IRON_INGOT)).isTrue();
            assertThat(decoded.getResultItem().getCount()).isEqualTo(2);
            assertThat(decoded.grindingTime()).isEqualTo(200);
            assertThat(decoded.experience()).isEqualTo(0.7f);
        }

        @Test
        @DisplayName("omits experience when default and decodes it back to the default")
        void defaultExperienceIsOptional() {
            MaceratorRecipeWrapper original = new MaceratorRecipeWrapper(
                Ingredient.of(Items.IRON_ORE),
                new ItemStackTemplate(Items.IRON_INGOT, 1),
                200,
                MaceratorRecipeWrapper.DEFAULT_EXPERIENCE
            );

            Tag encoded = MaceratorRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            assertThat(((CompoundTag) encoded).contains("experience")).isFalse();

            MaceratorRecipeWrapper decoded = MaceratorRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
            assertThat(decoded.experience()).isEqualTo(MaceratorRecipeWrapper.DEFAULT_EXPERIENCE);
        }
    }
}
