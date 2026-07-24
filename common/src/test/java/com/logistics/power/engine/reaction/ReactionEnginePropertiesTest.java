package com.logistics.power.engine.reaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

/**
 * Recipe-derived math for the Reaction Engine. RF/t is not stored — it's {@code energy / time} — so these
 * pin down the derivation and the recipe's field accessors and validation.
 */
class ReactionEnginePropertiesTest extends MinecraftTestEnvironment {

    private static ReactionRecipe recipe(int energy, int time) {
        return new ReactionRecipe(new FluidResult(Fluids.LAVA, 250), Ingredient.of(Items.BLAZE_POWDER), 1, energy, time);
    }

    @Test
    void outputPerTickIsEnergyOverTime() {
        assertThat(recipe(100_000, 500).outputPerTick()).isEqualTo(200L);
    }

    @Test
    void outputPerTickRoundsToNearest() {
        assertThat(recipe(1_000, 3).outputPerTick()).isEqualTo(333L); // 333.33 → 333
        assertThat(recipe(1_000, 300).outputPerTick()).isEqualTo(3L); // 3.33 → 3
    }

    @Test
    void outputPerTickIsAtLeastOne() {
        assertThat(recipe(10, 1_000).outputPerTick()).isEqualTo(1L); // 0.01 rounds to 0 → clamped to 1
    }

    @Test
    void accessorsExposeRecipeValues() {
        ReactionRecipe r = recipe(100_000, 500);
        assertThat(r.energy()).isEqualTo(100_000);
        assertThat(r.time()).isEqualTo(500);
        assertThat(r.reagentCount()).isEqualTo(1);
        assertThat(r.reactant().millibuckets()).isEqualTo(250);
        assertThat(r.reactant().fluid()).isEqualTo(Fluids.LAVA);
    }

    @Test
    void rejectsNonPositiveValues() {
        assertThatThrownBy(() -> recipe(0, 500)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recipe(100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReactionRecipe(
                        new FluidResult(Fluids.LAVA, 250), Ingredient.of(Items.BLAZE_POWDER), 0, 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
