package com.logistics.automation.alloysmelter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The Alloy Smelter's two inputs are unordered: a recipe matches regardless of which slot holds which. */
class AlloySmelterRecipeTest extends MinecraftTestEnvironment {

    /** copper x3 + gold x1 -> (stand-in) result; counts differ to prove per-slot consumption tracks placement. */
    private static AlloySmelterRecipe recipe() {
        return new AlloySmelterRecipe(
                Ingredient.of(Items.COPPER_INGOT), 3,
                Ingredient.of(Items.GOLD_INGOT), 1,
                ItemResult.of(Items.IRON_INGOT, 4), 4000, 0f, Optional.empty());
    }

    @Test
    void matchesInEitherInputOrder() {
        AlloySmelterRecipe recipe = recipe();
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 3);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        assertThat(recipe.matches(new DualRecipeInput(copper, gold), null)).isTrue();
        assertThat(recipe.matches(new DualRecipeInput(gold, copper), null)).isTrue();
    }

    @Test
    void consumptionFollowsPhysicalPlacement() {
        AlloySmelterRecipe recipe = recipe();
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 3);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        // input A (copper x3) in slot 0, input B (gold x1) in slot 1
        assertThat(recipe.consumptionForSlots(copper, gold)).containsExactly(3, 1);
        // swapped: gold in slot 0, copper in slot 1
        assertThat(recipe.consumptionForSlots(gold, copper)).containsExactly(1, 3);
    }

    @Test
    void requiresBothInputsPresentAndSufficient() {
        AlloySmelterRecipe recipe = recipe();

        // Only one input present.
        assertThat(recipe.matches(
                new DualRecipeInput(new ItemStack(Items.COPPER_INGOT, 3), ItemStack.EMPTY), null)).isFalse();
        // Right items, but copper count is short of the required 3.
        assertThat(recipe.matches(
                new DualRecipeInput(new ItemStack(Items.COPPER_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 1)), null))
                .isFalse();
    }

    /** Serializer codec round-trip: a field-name or default drift would surface here, not at data load. */
    @Nested
    @DisplayName("AlloySmelterRecipeSerializer")
    class Serializer {

        private RegistryOps<Tag> ops;

        @BeforeEach
        void setUp() {
            RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            ops = registries.createSerializationContext(NbtOps.INSTANCE);
        }

        @Test
        @DisplayName("round-trips both counted inputs, result, energy, and experience")
        void roundTripPreservesFields() {
            AlloySmelterRecipe decoded = roundTrip(recipe());

            assertThat(decoded.matches(
                    new DualRecipeInput(new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(Items.GOLD_INGOT, 1)), null))
                    .isTrue();
            assertThat(decoded.countA()).isEqualTo(3);
            assertThat(decoded.countB()).isEqualTo(1);
            assertThat(decoded.getResultItem().is(Items.IRON_INGOT)).isTrue();
            assertThat(decoded.getResultItem().getCount()).isEqualTo(4);
            assertThat(decoded.energy()).isEqualTo(4000);
            assertThat(decoded.experience()).isEqualTo(0f);
            assertThat(decoded.byproduct()).isEmpty();
        }

        @Test
        @DisplayName("round-trips a chance byproduct")
        void roundTripPreservesByproduct() {
            AlloySmelterRecipe original = new AlloySmelterRecipe(
                    Ingredient.of(Items.COPPER_INGOT), 3, Ingredient.of(Items.GOLD_INGOT), 1,
                    ItemResult.of(Items.IRON_INGOT, 4), 4000, 0f,
                    Optional.of(new AlloySmelterByproduct(Items.GOLD_NUGGET, 0.1f)));

            AlloySmelterRecipe decoded = roundTrip(original);

            assertThat(decoded.byproduct()).isPresent();
            assertThat(decoded.byproduct().get().item()).isEqualTo(Items.GOLD_NUGGET);
            assertThat(decoded.byproduct().get().chance()).isEqualTo(0.1f);
        }

        @Test
        @DisplayName("rejects a non-finite or negative byproduct chance on decode")
        void rejectsInvalidByproductChance() {
            assertThatThrownBy(() -> AlloySmelterByproduct.CODEC.parse(ops, byproductTag(-0.1f)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AlloySmelterByproduct.CODEC.parse(ops, byproductTag(Float.NaN)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        private Tag byproductTag(float chance) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", "minecraft:gold_nugget");
            tag.putFloat("chance", chance);
            return tag;
        }

        private AlloySmelterRecipe roundTrip(AlloySmelterRecipe original) {
            Tag encoded = AlloySmelterRecipeSerializer.CODEC.codec().encodeStart(ops, original).getOrThrow();
            return AlloySmelterRecipeSerializer.CODEC.codec().parse(ops, encoded).getOrThrow();
        }
    }
}
