package com.logistics.core.lib.storage;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

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

    // ==================== Components ====================
    // insert() gates on ItemStack.isSameItemSameComponents while extract() gates on
    // IItemKey.matches. The two only agree if the key is component-aware, as both loader keys
    // (ItemVariant / ItemResource) are.

    @Test
    @DisplayName("a key for a plain stack must not take a renamed one out of the container")
    void extract_doesNotMatchAcrossComponents() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, named(new ItemStack(Items.DIAMOND, 8), "Trophy"));
        container.setItem(1, new ItemStack(Items.DIAMOND, 8));
        ContainerItemStorage storage = new ContainerItemStorage(container);
        IItemKey plain = ItemStorageLookup.of(new ItemStack(Items.DIAMOND));

        assertThat(storage.extract(plain, 16, false))
                .as("only the plain diamonds answer to a plain-diamond key")
                .isEqualTo(8);
        assertThat(container.getItem(0).getCount())
                .as("the renamed diamonds must be left alone")
                .isEqualTo(8);
        assertThat(container.getItem(1).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("insert re-creates the components the key was made from, and stacks onto them")
    void insert_preservesComponentsAndStacks() {
        SimpleContainer container = new SimpleContainer(2);
        ContainerItemStorage storage = new ContainerItemStorage(container);
        IItemKey trophy = ItemStorageLookup.of(named(new ItemStack(Items.DIAMOND), "Trophy"));

        assertThat(storage.insert(trophy, 4, false)).isEqualTo(4);
        assertThat(container.getItem(0).get(DataComponents.CUSTOM_NAME))
                .as("a round trip through the key must not strip the name")
                .isEqualTo(Component.literal("Trophy"));

        assertThat(storage.insert(trophy, 4, false)).isEqualTo(4);
        assertThat(container.getItem(0).getCount())
                .as("the second insert stacks onto the matching slot")
                .isEqualTo(8);
        assertThat(container.getItem(1).isEmpty()).isTrue();
    }

    // ==================== Vanilla's two-check placement contract ====================
    // HopperBlockEntity.canPlaceItemInContainer requires Container.canPlaceItem AND, for a
    // WorldlyContainer, canPlaceItemThroughFace. A container that validates in only one of the
    // two must still be respected.

    @Test
    @DisplayName("a sided insert respects canPlaceItem even when the face check passes")
    void sidedInsert_respectsCanPlaceItem() {
        OpenFaceContainer container = new OpenFaceContainer(2);
        ContainerItemStorage storage = new ContainerItemStorage(container, Direction.UP);
        IItemKey diamond = ItemStorageLookup.of(new ItemStack(Items.DIAMOND));

        assertThat(storage.insert(diamond, 8, false))
                .as("the container refuses this item in canPlaceItem, so nothing may go in")
                .isZero();
        assertThat(container.getItem(0).isEmpty()).isTrue();
        assertThat(container.getItem(1).isEmpty()).isTrue();
    }

    /** Exposes every slot to every face and waves the face check through, refusing only in canPlaceItem. */
    private static final class OpenFaceContainer extends SimpleContainer implements WorldlyContainer {
        private OpenFaceContainer(int size) {
            super(size);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return IntStream.range(0, getContainerSize()).toArray();
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }
    }

    private static ItemStack named(ItemStack stack, String name) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return copy;
    }
}
