package com.logistics.core.lib.storage;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContainerItemStorage")
class ContainerItemStorageTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("contents exposes each non-empty container slot")
    void contents_exposesEachNonEmptyContainerSlot() {
        SimpleContainer container = new SimpleContainer(3);
        container.setItem(0, new ItemStack(Items.DIAMOND, 4));
        container.setItem(1, new ItemStack(Items.IRON_INGOT, 7));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        assertThat(storage).isInstanceOf(ISlottedItemStorage.class);
        assertThat(storage.slotCount()).isEqualTo(3);
        assertThat(storage.slotView(0).resource().matches(new ItemStack(Items.DIAMOND))).isTrue();
        assertThat(storage.slotView(0).amount()).isEqualTo(4);
        assertThat(storage.slotView(1).resource().matches(new ItemStack(Items.IRON_INGOT))).isTrue();
        assertThat(storage.slotView(1).amount()).isEqualTo(7);
        assertThat(storage.slotView(2)).isNull();
        assertThat(storage.contents()).hasSize(2);
    }

    @Test
    @DisplayName("slot insert and extract target only the requested slot")
    void slotInsertAndExtract_targetOnlyRequestedSlot() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, new ItemStack(Items.DIAMOND, 60));
        ContainerItemStorage storage = new ContainerItemStorage(container);
        IItemKey diamond = ItemStorageLookup.of(new ItemStack(Items.DIAMOND));
        IItemKey iron = ItemStorageLookup.of(new ItemStack(Items.IRON_INGOT));

        assertThat(storage.insert(0, diamond, 10, false)).isEqualTo(4);
        assertThat(container.getItem(0).getCount()).isEqualTo(64);
        assertThat(storage.insert(0, iron, 10, false)).isZero();
        assertThat(storage.insert(1, iron, 10, true)).isEqualTo(10);
        assertThat(container.getItem(1).isEmpty()).isTrue();
        assertThat(storage.insert(1, iron, 10, false)).isEqualTo(10);
        assertThat(container.getItem(1).is(Items.IRON_INGOT)).isTrue();
        assertThat(container.getItem(1).getCount()).isEqualTo(10);

        assertThat(storage.extract(0, iron, 5, false)).isZero();
        assertThat(storage.extract(1, iron, 5, true)).isEqualTo(5);
        assertThat(container.getItem(1).getCount()).isEqualTo(10);
        assertThat(storage.extract(1, iron, 5, false)).isEqualTo(5);
        assertThat(container.getItem(1).getCount()).isEqualTo(5);
    }
}
