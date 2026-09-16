package com.logistics.core.lib.storage;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CroppedItemStorage hides the cropped slots from every query")
class CroppedItemStorageTest extends MinecraftTestEnvironment {

    private static final IItemKey COBBLE = ItemStorageLookup.of(new ItemStack(Items.COBBLESTONE));

    private static ContainerItemStorage filled(int size, int... slots) {
        SimpleContainer container = new SimpleContainer(size);
        for (int slot : slots) container.setItem(slot, new ItemStack(Items.COBBLESTONE, 16));
        return new ContainerItemStorage(container);
    }

    @Test
    @DisplayName("an uncropped request returns the storage untouched")
    void noCrop_returnsTheSameStorage() {
        ContainerItemStorage storage = filled(3, 0, 1, 2);

        assertThat(CroppedItemStorage.of(storage, 0, 0)).isSameAs(storage);
    }

    @Test
    @DisplayName("the crop counts slots, not occupied slots")
    void crop_countsSlotsNotStacks() {
        // Slots 0 and 2 are empty; cropping one from each end must hide those, not the stacks.
        ISlottedItemStorage cropped = (ISlottedItemStorage) CroppedItemStorage.of(filled(4, 1, 3), 1, 1);

        assertThat(cropped.slotCount()).isEqualTo(2);
        assertThat(cropped.slotView(0)).as("exposed slot 0 is the delegate's slot 1").isNotNull();
        assertThat(cropped.slotView(1)).as("exposed slot 1 is the delegate's empty slot 2").isNull();
        assertThat(amounts(cropped)).containsExactly(16L);
    }

    @Test
    @DisplayName("a crop wider than the storage leaves an empty view rather than clamping")
    void crop_widerThanTheStorageIsEmpty() {
        ISlottedItemStorage cropped = (ISlottedItemStorage) CroppedItemStorage.of(filled(2, 0, 1), 1, 1);

        assertThat(cropped.slotCount()).isZero();
        assertThat(cropped.contents()).isEmpty();
        assertThat(cropped.extract(COBBLE, 64, false)).isZero();
        assertThat(cropped.insert(COBBLE, 64, false)).isZero();
        assertThatThrownBy(() -> cropped.slotView(0)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("a resource-scoped extract cannot reach a cropped slot")
    void resourceExtract_staysInsideTheCrop() {
        SimpleContainer container = new SimpleContainer(3);
        for (int slot = 0; slot < 3; slot++) container.setItem(slot, new ItemStack(Items.COBBLESTONE, 16));
        IItemStorage cropped = CroppedItemStorage.of(new ContainerItemStorage(container), 1, 1);

        assertThat(cropped.extract(COBBLE, 64, false))
                .as("only the middle slot is reachable")
                .isEqualTo(16);
        assertThat(container.getItem(0).getCount()).isEqualTo(16);
        assertThat(container.getItem(1).isEmpty()).isTrue();
        assertThat(container.getItem(2).getCount()).isEqualTo(16);
    }

    @Test
    @DisplayName("a resource-scoped insert cannot reach a cropped slot")
    void resourceInsert_staysInsideTheCrop() {
        SimpleContainer container = new SimpleContainer(3);
        IItemStorage cropped = CroppedItemStorage.of(new ContainerItemStorage(container), 1, 1);

        assertThat(cropped.insert(COBBLE, 128, false))
                .as("one reachable slot, so one stack's worth")
                .isEqualTo(64);
        assertThat(container.getItem(0).isEmpty()).isTrue();
        assertThat(container.getItem(1).getCount()).isEqualTo(64);
        assertThat(container.getItem(2).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("slot indices address the crop, not the delegate")
    void slotAccess_isRebased() {
        SimpleContainer container = new SimpleContainer(3);
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
        ISlottedItemStorage cropped =
                (ISlottedItemStorage) CroppedItemStorage.of(new ContainerItemStorage(container), 1, 0);

        assertThat(cropped.extract(0, COBBLE, 16, false)).isEqualTo(16);
        assertThat(container.getItem(1).isEmpty()).as("exposed slot 0 drained the delegate's slot 1").isTrue();
        assertThatThrownBy(() -> cropped.extract(2, COBBLE, 1, false))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("a storage with no slots keeps the crop off entirely")
    void nonSlottedStorage_isNotCropped() {
        IItemStorage resourceScoped = new ResourceScopedStorage();

        assertThat(CroppedItemStorage.of(resourceScoped, 1, 1))
                .as("slot-index semantics are meaningless here, so RESERVE/GUARDED must not pretend")
                .isSameAs(resourceScoped);
    }

    private static List<Long> amounts(IItemStorage storage) {
        List<Long> amounts = new ArrayList<>();
        for (IItemView view : storage.contents()) amounts.add(view.amount());
        return amounts;
    }

    /** An {@link IItemStorage} with no slot identity at all — the case the crop has to decline. */
    private static final class ResourceScopedStorage implements IItemStorage {
        @Override public long insert(IItemKey item, long maxAmount, boolean simulate) { return 0; }
        @Override public long extract(IItemKey item, long maxAmount, boolean simulate) { return 0; }
        @Override public Iterable<IItemView> contents() { return List.of(); }
    }
}
