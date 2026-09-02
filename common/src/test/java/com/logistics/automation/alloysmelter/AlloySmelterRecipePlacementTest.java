package com.logistics.automation.alloysmelter;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineData;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Clicking a recipe in the Alloy Smelter's recipe book must conserve items: whatever leaves the
 * input slots has to turn up in the player's inventory or on the ground, never in both.
 */
class AlloySmelterRecipePlacementTest extends MinecraftTestEnvironment {

    private static final int PLAYER_SLOTS = 36;

    /** copper x3 + gold x1 -> (stand-in) result. */
    private static RecipeHolder<AlloySmelterRecipe> recipe() {
        AlloySmelterRecipe recipe = new AlloySmelterRecipe(
                Ingredient.of(Items.COPPER_INGOT), 3,
                Ingredient.of(Items.GOLD_INGOT), 1,
                ItemResult.of(Items.IRON_INGOT, 4), 4000, 0f, Optional.empty());
        ResourceKey<Recipe<?>> id =
                ResourceKey.create(Registries.RECIPE, ResourceId.in("logistics", "test").toIdentifier());
        return new RecipeHolder<>(id, recipe);
    }

    private static AlloySmelterScreenHandler menu(Inventory inventory, SimpleContainer machine) {
        return new AlloySmelterScreenHandler(1, inventory, machine, new SimpleContainerData(MachineData.COUNT));
    }

    /** Every item the player can reach: inventory (incl. equipment), machine slots, and the ground. */
    private static int totalItems(TestInventory inventory, SimpleContainer machine) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            total += inventory.getItem(i).getCount();
        }
        for (int i = 0; i < machine.getContainerSize(); i++) {
            total += machine.getItem(i).getCount();
        }
        for (ItemStack stack : inventory.dropped) {
            total += stack.getCount();
        }
        return total;
    }

    @Test
    @DisplayName("a recipe click with a full inventory drops the input instead of duplicating it")
    void recipeClickWithFullInventoryConservesItems() {
        TestInventory inventory = new TestInventory();
        // No free slot and nothing copper can stack onto, so vanilla has to drop the returned input.
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            inventory.setItem(i, new ItemStack(Items.STONE, 64));
        }
        SimpleContainer machine = new SimpleContainer(AlloySmelterBlockEntity.TOTAL_SLOTS);
        machine.setItem(AlloySmelterBlockEntity.INPUT_A_SLOT, new ItemStack(Items.COPPER_INGOT, 3));

        int before = totalItems(inventory, machine);
        menu(inventory, machine).handlePlacement(false, false, recipe(), null, inventory);

        assertThat(totalItems(inventory, machine)).isEqualTo(before);
        assertThat(inventory.dropped).singleElement().satisfies(stack -> {
            assertThat(stack.is(Items.COPPER_INGOT)).isTrue();
            assertThat(stack.getCount()).isEqualTo(3);
        });
        assertThat(machine.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("an uncraftable recipe click returns the inputs to a player with room")
    void uncraftableRecipeClickReturnsInputs() {
        TestInventory inventory = new TestInventory();
        SimpleContainer machine = new SimpleContainer(AlloySmelterBlockEntity.TOTAL_SLOTS);
        machine.setItem(AlloySmelterBlockEntity.INPUT_A_SLOT, new ItemStack(Items.COPPER_INGOT, 3));

        int before = totalItems(inventory, machine);
        menu(inventory, machine).handlePlacement(false, false, recipe(), null, inventory);

        assertThat(totalItems(inventory, machine)).isEqualTo(before);
        assertThat(inventory.dropped).isEmpty();
        assertThat(machine.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT).isEmpty()).isTrue();
        assertThat(inventory.countOf(Items.COPPER_INGOT)).isEqualTo(3);
    }

    @Test
    @DisplayName("a craftable recipe click moves both inputs out of the inventory exactly once")
    void craftableRecipeClickConservesItems() {
        TestInventory inventory = new TestInventory();
        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 10));
        inventory.setItem(1, new ItemStack(Items.GOLD_INGOT, 10));
        SimpleContainer machine = new SimpleContainer(AlloySmelterBlockEntity.TOTAL_SLOTS);

        int before = totalItems(inventory, machine);
        menu(inventory, machine).handlePlacement(false, false, recipe(), null, inventory);

        assertThat(totalItems(inventory, machine)).isEqualTo(before);
        assertThat(inventory.dropped).isEmpty();
        assertThat(machine.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT).getCount()).isEqualTo(3);
        assertThat(machine.getItem(AlloySmelterBlockEntity.INPUT_B_SLOT).getCount()).isEqualTo(1);
        assertThat(inventory.countOf(Items.COPPER_INGOT)).isEqualTo(7);
        assertThat(inventory.countOf(Items.GOLD_INGOT)).isEqualTo(9);
    }

    @Test
    @DisplayName("filling an input slot never pushes it past its stack limit")
    void fillSlotRespectsTheSlotStackLimit() {
        TestInventory inventory = new TestInventory();
        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 64));
        SimpleContainer machine = new SimpleContainer(AlloySmelterBlockEntity.TOTAL_SLOTS);
        machine.setItem(AlloySmelterBlockEntity.INPUT_A_SLOT, new ItemStack(Items.COPPER_INGOT, 30));
        Slot slot = new Slot(machine, AlloySmelterBlockEntity.INPUT_A_SLOT, 0, 0);

        boolean filled = AlloySmelterScreenHandler.fillSlot(
                inventory, slot, Items.COPPER_INGOT.builtInRegistryHolder(), 63);

        assertThat(filled).isFalse();
        assertThat(machine.getItem(AlloySmelterBlockEntity.INPUT_A_SLOT).getCount()).isEqualTo(64);
        assertThat(inventory.countOf(Items.COPPER_INGOT)).isEqualTo(30);
    }

    /**
     * A headless player inventory: vanilla's own slot-finding logic, with {@code Player.drop} recorded
     * instead of spawning an entity. Mirrors {@code Inventory#placeItemBackInInventory} exactly —
     * including that the dropped stack is handed over at full count, not emptied.
     */
    private static final class TestInventory extends Inventory {

        final List<ItemStack> dropped = new ArrayList<>();

        TestInventory() {
            super(null, new EntityEquipment());
        }

        int countOf(net.minecraft.world.item.Item item) {
            int total = 0;
            for (int i = 0; i < getContainerSize(); i++) {
                if (getItem(i).is(item)) {
                    total += getItem(i).getCount();
                }
            }
            return total;
        }

        @Override
        public void placeItemBackInInventory(ItemStack stack, boolean sendPacket) {
            while (!stack.isEmpty()) {
                int index = getSlotWithRemainingSpace(stack);
                if (index == -1) {
                    index = getFreeSlot();
                }
                if (index == -1) {
                    dropped.add(stack);
                    return;
                }
                ItemStack existing = getItem(index);
                ItemStack moved = stack.split(stack.getMaxStackSize() - existing.getCount());
                if (existing.isEmpty()) {
                    setItem(index, moved);
                } else {
                    existing.grow(moved.getCount());
                }
            }
        }
    }
}
