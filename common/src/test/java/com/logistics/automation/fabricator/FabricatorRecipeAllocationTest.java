package com.logistics.automation.fabricator;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Greedy per-ingredient/per-slot allocation used by the Fabricator to check and consume its inputs. */
@DisplayName("FabricatorRecipe.allocateFrom")
class FabricatorRecipeAllocationTest extends MinecraftTestEnvironment {

    private record Take(int slot, int amount) {}

    private record Result(boolean satisfied, List<Take> takes) {}

    private static SizedIngredient ing(ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    private static FabricatorRecipe recipe(SizedIngredient... ingredients) {
        return new FabricatorRecipe(List.of(ingredients), ItemResult.of(Items.IRON_INGOT, 1), 1000);
    }

    private static Result allocate(FabricatorRecipe recipe, ItemStack... pool) {
        List<Take> takes = new ArrayList<>();
        boolean ok = recipe.allocateFrom(List.of(pool), (slot, amount) -> takes.add(new Take(slot, amount)));
        return new Result(ok, takes);
    }

    @Test
    @DisplayName("a single ingredient is satisfied from one slot that holds enough")
    void singleIngredient_fromOneSlot() {
        Result r = allocate(recipe(ing(Items.COPPER_INGOT, 3)), new ItemStack(Items.COPPER_INGOT, 5));
        assertThat(r.satisfied()).isTrue();
        assertThat(r.takes()).containsExactly(new Take(0, 3));
    }

    @Test
    @DisplayName("a single ingredient is drawn across multiple matching slots in order")
    void singleIngredient_splitAcrossSlots() {
        Result r = allocate(
                recipe(ing(Items.COPPER_INGOT, 5)),
                new ItemStack(Items.COPPER_INGOT, 2),
                new ItemStack(Items.COPPER_INGOT, 4));
        assertThat(r.satisfied()).isTrue();
        assertThat(r.takes()).containsExactly(new Take(0, 2), new Take(1, 3));
    }

    @Test
    @DisplayName("distinct ingredients each pull from their own matching slot")
    void multipleIngredients_distinctSlots() {
        Result r = allocate(
                recipe(ing(Items.COPPER_INGOT, 3), ing(Items.GOLD_INGOT, 1)),
                new ItemStack(Items.COPPER_INGOT, 3),
                new ItemStack(Items.GOLD_INGOT, 1));
        assertThat(r.satisfied()).isTrue();
        assertThat(r.takes()).containsExactly(new Take(0, 3), new Take(1, 1));
    }

    @Test
    @DisplayName("allocation fails when a slot lacks the required count")
    void insufficientCount_fails() {
        Result r = allocate(recipe(ing(Items.COPPER_INGOT, 3)), new ItemStack(Items.COPPER_INGOT, 2));
        assertThat(r.satisfied()).isFalse();
    }

    @Test
    @DisplayName("allocation fails and takes nothing when no slot matches the ingredient")
    void wrongItem_takesNothing() {
        Result r = allocate(recipe(ing(Items.COPPER_INGOT, 1)), new ItemStack(Items.GOLD_INGOT, 5));
        assertThat(r.satisfied()).isFalse();
        assertThat(r.takes()).isEmpty();
    }

    @Test
    @DisplayName("two ingredients of the same item need the combined count, never double-counting units")
    void sameItemTwoIngredients_needsCombinedCount() {
        FabricatorRecipe recipe = recipe(ing(Items.COPPER_INGOT, 1), ing(Items.COPPER_INGOT, 1));
        assertThat(allocate(recipe, new ItemStack(Items.COPPER_INGOT, 1)).satisfied()).isFalse();
        assertThat(allocate(recipe, new ItemStack(Items.COPPER_INGOT, 2)).satisfied()).isTrue();
    }

    @Test
    @DisplayName("an empty pool cannot satisfy any ingredient")
    void emptyPool_fails() {
        assertThat(allocate(recipe(ing(Items.COPPER_INGOT, 1))).satisfied()).isFalse();
    }

    @Test
    @DisplayName("canCraftFrom mirrors allocateFrom's verdict without side effects")
    void canCraftFrom_mirrorsVerdict() {
        FabricatorRecipe recipe = recipe(ing(Items.COPPER_INGOT, 3));
        assertThat(recipe.canCraftFrom(List.of(new ItemStack(Items.COPPER_INGOT, 3)))).isTrue();
        assertThat(recipe.canCraftFrom(List.of(new ItemStack(Items.COPPER_INGOT, 2)))).isFalse();
    }

    @Test
    @DisplayName("a failed multi-ingredient allocation may already have reported partial takes")
    void failedAllocation_mayReportPartialTakes() {
        // Copper is satisfied first, then gold is missing: the copper takes are already reported, so
        // callers using the sink to consume must treat a false result as a rollback signal.
        Result r = allocate(
                recipe(ing(Items.COPPER_INGOT, 3), ing(Items.GOLD_INGOT, 1)),
                new ItemStack(Items.COPPER_INGOT, 3));
        assertThat(r.satisfied()).isFalse();
        assertThat(r.takes()).containsExactly(new Take(0, 3));
    }
}
