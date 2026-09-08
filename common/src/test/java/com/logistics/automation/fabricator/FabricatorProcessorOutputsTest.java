package com.logistics.automation.fabricator;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.fabricator.FabricatorProcessorComponent.Output;
import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SidedLayout;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The output list is the fabricator GUI's only feed: an entry the player can see is an entry the
 * player can click, and clicking toggles selection. So what {@code outputs()} lists decides both
 * what can be queued and what can be cancelled.
 */
@DisplayName("FabricatorProcessorComponent.outputs")
class FabricatorProcessorOutputsTest extends MinecraftTestEnvironment {

    private static final int INPUT_SLOTS = 12;

    private static final ResourceId GEAR = ResourceId.in("logistics", "fabricator/gear");
    private static final ResourceId PLATE = ResourceId.in("logistics", "fabricator/plate");

    private ItemStoreComponent items;
    private FabricatorProcessorComponent processor;
    private RecipeManager recipes;

    private void rig() {
        Runnable noop = () -> {};
        SlotRole[] roles = new SlotRole[INPUT_SLOTS];
        Arrays.fill(roles, SlotRole.INPUT);
        int[] inputSlots = new int[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inputSlots[i] = i;
        }
        items = new ItemStoreComponent("items", roles, SidedLayout.topIn(inputSlots, stack -> true), noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        processor = new FabricatorProcessorComponent("processor", items, energy, 80, (ctx, lit) -> {}, noop);
        recipes = fakeRecipeManager(
                holder(GEAR, recipe(Items.IRON_INGOT, 4, Items.IRON_BLOCK)),
                holder(PLATE, recipe(Items.GOLD_INGOT, 2, Items.COMPARATOR)));
    }

    @Test
    @DisplayName("a queued recipe stays listed after its materials run out, so it can still be cancelled")
    void queuedRecipeStaysListedWithoutMaterials() {
        rig();
        items.container().setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        processor.toggle(GEAR);
        assertThat(statesById(processor.outputs(recipes)))
                .containsEntry(GEAR, FabricatorProcessorComponent.STATE_QUEUED);

        // The player reroutes the iron feed elsewhere; the queue itself is untouched.
        items.container().setItem(0, ItemStack.EMPTY);

        assertThat(statesById(processor.outputs(recipes)))
                .as("a queued recipe must remain clickable so the order can be cancelled")
                .containsEntry(GEAR, FabricatorProcessorComponent.STATE_QUEUED);
    }

    @Test
    @DisplayName("an unqueued recipe is hidden while its materials are absent")
    void unqueuedRecipeStaysHiddenWithoutMaterials() {
        rig();
        assertThat(statesById(processor.outputs(recipes))).doesNotContainKey(PLATE);
    }

    @Test
    @DisplayName("a recipe whose materials are present is listed as available until it is queued")
    void craftableRecipeIsListedAsAvailable() {
        rig();
        items.container().setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
        assertThat(statesById(processor.outputs(recipes)))
                .containsEntry(PLATE, FabricatorProcessorComponent.STATE_AVAILABLE);
    }

    @Test
    @DisplayName("cancelling a queued-but-unavailable recipe drops it from the queue and the list")
    void cancellingAStalledOrderClearsIt() {
        rig();
        processor.toggle(GEAR);
        assertThat(statesById(processor.outputs(recipes))).containsKey(GEAR);

        processor.toggle(GEAR);

        assertThat(statesById(processor.outputs(recipes))).doesNotContainKey(GEAR);
    }

    // ----- helpers -----

    private static java.util.Map<ResourceId, Integer> statesById(List<Output> outputs) {
        java.util.Map<ResourceId, Integer> byId = new java.util.LinkedHashMap<>();
        for (Output output : outputs) {
            byId.put(output.id(), output.state());
        }
        return byId;
    }

    private static FabricatorRecipe recipe(ItemLike ingredient, int count, ItemLike result) {
        return new FabricatorRecipe(
                List.of(new SizedIngredient(Ingredient.of(ingredient), count)), ItemResult.of(result, 1), 1000);
    }

    private static RecipeHolder<FabricatorRecipe> holder(ResourceId id, FabricatorRecipe recipe) {
        // 1.21.1 keys a RecipeHolder by ResourceLocation; 26.x keys it by ResourceKey<Recipe<?>>.
        return new RecipeHolder<>(ResourceLocation.parse(id.toString()), recipe);
    }

    /** A RecipeManager that serves a fixed recipe list — the component only ever reads {@code getRecipes()}. */
    @SafeVarargs
    private static RecipeManager fakeRecipeManager(RecipeHolder<FabricatorRecipe>... holders) {
        List<RecipeHolder<?>> all = new ArrayList<>(Arrays.asList(holders));
        return new RecipeManager(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)) {
            @Override
            public Collection<RecipeHolder<?>> getRecipes() {
                return all;
            }
        };
    }
}
