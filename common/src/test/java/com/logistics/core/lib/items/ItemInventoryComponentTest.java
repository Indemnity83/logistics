package com.logistics.core.lib.items;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemInventoryComponent")
class ItemInventoryComponentTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("new inventories expose fixed size and start empty")
    void newInventory_isEmpty() {
        ItemInventoryComponent inventory = inventory(3, () -> {});

        assertThat(inventory.getContainerSize()).isEqualTo(3);
        assertThat(inventory.isEmpty()).isTrue();
        assertThat(inventory.getItem(0)).isEqualTo(ItemStack.EMPTY);
        assertThat(inventory.stillValid(null)).isTrue();
    }

    @Test
    @DisplayName("setItem stores stack and fires change callback")
    void setItem_storesStackAndFiresCallback() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(2, changes::incrementAndGet);

        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));

        assertThat(inventory.isEmpty()).isFalse();
        assertThat(inventory.getItem(0).is(Items.DIAMOND)).isTrue();
        assertThat(inventory.getItem(0).getCount()).isEqualTo(5);
        assertThat(changes).hasValue(1);
    }

    @Test
    @DisplayName("removeItem splits partial stacks and fires callback")
    void removeItem_splitsPartialStack() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(1, changes::incrementAndGet);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));

        ItemStack removed = inventory.removeItem(0, 2);

        assertThat(removed.getCount()).isEqualTo(2);
        assertThat(inventory.getItem(0).getCount()).isEqualTo(3);
        assertThat(changes).hasValue(2);
    }

    @Test
    @DisplayName("removeItem clears whole stacks through setItem")
    void removeItem_clearsWholeStack() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(1, changes::incrementAndGet);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));

        ItemStack removed = inventory.removeItem(0, 5);

        assertThat(removed.getCount()).isEqualTo(5);
        assertThat(inventory.getItem(0).isEmpty()).isTrue();
        assertThat(changes).hasValue(2);
    }

    @Test
    @DisplayName("removeItemNoUpdate clears slot without callback")
    void removeItemNoUpdate_clearsWithoutCallback() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(1, changes::incrementAndGet);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));
        changes.set(0);

        ItemStack removed = inventory.removeItemNoUpdate(0);

        assertThat(removed.getCount()).isEqualTo(5);
        assertThat(inventory.getItem(0).isEmpty()).isTrue();
        assertThat(changes).hasValue(0);
    }

    @Test
    @DisplayName("clearContent and setChanged fire callbacks")
    void clearContentAndSetChanged_fireCallbacks() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(1, changes::incrementAndGet);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));

        inventory.clearContent();
        inventory.setChanged();

        assertThat(inventory.isEmpty()).isTrue();
        assertThat(changes).hasValue(3);
    }

    @Test
    @DisplayName("removeItem on empty slot returns empty without callback")
    void removeItem_emptySlot_returnsEmptyWithoutCallback() {
        AtomicInteger changes = new AtomicInteger();
        ItemInventoryComponent inventory = inventory(1, changes::incrementAndGet);

        assertThat(inventory.removeItem(0, 1)).isEqualTo(ItemStack.EMPTY);
        assertThat(changes).hasValue(0);
    }

    private static ItemInventoryComponent inventory(int size, Runnable onChanged) {
        return new ItemInventoryComponent(size, onChanged);
    }
}
