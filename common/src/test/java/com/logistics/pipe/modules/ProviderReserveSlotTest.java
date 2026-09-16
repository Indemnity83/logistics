package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.modules.ProviderModule.ProviderMode;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The crop modes name the slots they protect, by index — not by whatever happens to be occupied.
 * Extraction has to drain the slots the crop selected, not just the right total from wherever the
 * storage happens to hand it over.
 */
@DisplayName("Provider crop modes protect the slots they name")
class ProviderReserveSlotTest extends MinecraftTestEnvironment {

    private static final IItemKey COBBLE = ItemStorageLookup.of(new ItemStack(Items.COBBLESTONE));
    private static final IItemKey DIRT = ItemStorageLookup.of(new ItemStack(Items.DIRT));
    private static final IItemKey STONE = ItemStorageLookup.of(new ItemStack(Items.STONE));
    private static final IItemKey GLASS = ItemStorageLookup.of(new ItemStack(Items.GLASS));

    private static ProviderModule module() {
        return new ProviderModule(64, 64);
    }

    @Test
    @DisplayName("Leave First Slot does not drain the first slot")
    void reserve_leavesTheFirstSlotAlone() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 32));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        // The supply scan advertises only slot 1's 32, so that is all a requester can ask for.
        long extracted = module().extractItems(storage, COBBLE, 32, ProviderMode.RESERVE, false);

        assertThat(extracted).isEqualTo(32);
        assertThat(container.getItem(0).getCount())
                .as("slot 0 is the slot the player asked the Provider to leave alone")
                .isEqualTo(32);
        assertThat(container.getItem(1).isEmpty())
                .as("slot 1 is the only slot in scope, so it is the one that should drain")
                .isTrue();
    }

    @Test
    @DisplayName("Leave First & Last Slot drains only the middle")
    void guarded_leavesBothEndsAlone() {
        SimpleContainer container = new SimpleContainer(3);
        for (int slot = 0; slot < 3; slot++) {
            container.setItem(slot, new ItemStack(Items.COBBLESTONE, 32));
        }
        ContainerItemStorage storage = new ContainerItemStorage(container);

        long extracted = module().extractItems(storage, COBBLE, 32, ProviderMode.GUARDED, false);

        assertThat(extracted).isEqualTo(32);
        assertThat(container.getItem(0).getCount()).as("first slot is guarded").isEqualTo(32);
        assertThat(container.getItem(1).isEmpty()).as("only the middle is in scope").isTrue();
        assertThat(container.getItem(2).getCount()).as("last slot is guarded").isEqualTo(32);
    }

    @Test
    @DisplayName("Leave 1 Per Slot leaves one in every slot it drains")
    void seeded_leavesOneInEverySlot() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 10));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        // 9 from each slot; the last item in each is the seed stock the mode promises to keep.
        long extracted = module().extractItems(storage, COBBLE, 18, ProviderMode.SEEDED, false);

        assertThat(extracted).isEqualTo(18);
        assertThat(container.getItem(0).getCount()).as("slot 0 keeps its seed").isEqualTo(1);
        assertThat(container.getItem(1).getCount()).as("slot 1 keeps its seed").isEqualTo(1);
    }

    // ==================== Slot index, not occupancy ====================
    // The crop counts real slot indices. The fixtures above fill every slot, so they cannot tell
    // the two readings apart; these leave gaps, where they diverge.

    @Test
    @DisplayName("an empty first slot is still the slot Leave First Slot protects — so it protects nothing")
    void reserve_protectsSlotZeroEvenWhenItIsEmpty() {
        SimpleContainer container = new SimpleContainer(5);
        container.setItem(3, new ItemStack(Items.COBBLESTONE, 32));
        container.setItem(4, new ItemStack(Items.COBBLESTONE, 32));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        long extracted = module().extractItems(storage, COBBLE, 64, ProviderMode.RESERVE, false);

        assertThat(extracted)
                .as("slot 0 is the protected slot and it is empty, so nothing is held back")
                .isEqualTo(64);
        assertThat(container.getItem(3).isEmpty()).isTrue();
        assertThat(container.getItem(4).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Leave First & Last Slot guards the container's last slot, not the last occupied one")
    void guarded_guardsTheContainersLastSlot() {
        SimpleContainer container = new SimpleContainer(5);
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 32));
        container.setItem(2, new ItemStack(Items.COBBLESTONE, 32));
        container.setItem(3, new ItemStack(Items.COBBLESTONE, 32));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        long extracted = module().extractItems(storage, COBBLE, 96, ProviderMode.GUARDED, false);

        assertThat(extracted)
                .as("slots 0 and 4 are the guarded ones and both are empty, so all stock is in scope")
                .isEqualTo(96);
        assertThat(container.getItem(1).isEmpty()).isTrue();
        assertThat(container.getItem(2).isEmpty()).isTrue();
        assertThat(container.getItem(3).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("slot 0 is the first slot the accessed face exposes, not the container's slot 0")
    void reserve_countsFromTheFirstFaceAccessibleSlot() {
        FacedContainer container = new FacedContainer(5, 2, 3);
        for (int slot = 0; slot < 5; slot++) {
            container.setItem(slot, new ItemStack(Items.COBBLESTONE, 32));
        }
        ContainerItemStorage storage = new ContainerItemStorage(container, Direction.NORTH);

        long extracted = module().extractItems(storage, COBBLE, 64, ProviderMode.RESERVE, false);

        assertThat(extracted)
                .as("the face exposes slots 2 and 3; the crop hides 2, leaving only 3")
                .isEqualTo(32);
        assertThat(container.getItem(2).getCount())
                .as("slot 2 is the face's first slot, so it is the one RESERVE protects")
                .isEqualTo(32);
        assertThat(container.getItem(3).isEmpty()).as("slot 3 is the only slot in scope").isTrue();
        assertThat(container.getItem(0).getCount()).as("slot 0 is not reachable from this face").isEqualTo(32);
    }

    @Test
    @DisplayName("a container no wider than the crop offers nothing at all")
    void guarded_onAContainerTooSmallToCropOffersNothing() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 32));
        ContainerItemStorage storage = new ContainerItemStorage(container);

        assertThat(advertised(storage, ProviderMode.GUARDED))
                .as("both slots are guarded, so there is nothing left to advertise")
                .isEmpty();
        assertThat(module().extractItems(storage, COBBLE, 64, ProviderMode.GUARDED, false)).isZero();
        assertThat(container.getItem(0).getCount()).isEqualTo(32);
        assertThat(container.getItem(1).getCount()).isEqualTo(32);
    }

    @Test
    @DisplayName("a storage with no slots gets no crop at all — RESERVE behaves as SUPPLY")
    void reserve_isDisabledOnAStorageWithoutSlots() {
        ResourceScopedStorage storage = new ResourceScopedStorage(64);

        assertThat(module().extractItems(storage, COBBLE, 64, ProviderMode.RESERVE, false))
                .as("nothing here has a slot index, so the crop must not silently mean something else")
                .isEqualTo(64);
        assertThat(storage.amount).isZero();
    }

    // ==================== One cropped view behind both paths ====================

    /**
     * The invariant the duplicated crop cost us in #934: what the Provider advertises and what it
     * will hand over must name the same slots. Both paths read
     * {@link ProviderModule#croppedView} and nothing else, so this compares each of them against
     * that one view rather than only against each other.
     *
     * <p>Each slot holds a distinct item, so the set of item keys is a faithful name for the set of
     * slots in scope.
     */
    @Test
    @DisplayName("the advertised supply and the extraction both see exactly the cropped view's slots")
    void scanAndExtract_seeTheSameCroppedSlots() {
        for (ProviderMode mode : List.of(ProviderMode.SUPPLY, ProviderMode.RESERVE, ProviderMode.GUARDED)) {
            // Gaps at 2 and 4 so slot index and occupancy index disagree.
            SimpleContainer container = new SimpleContainer(6);
            container.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
            container.setItem(1, new ItemStack(Items.DIRT, 8));
            container.setItem(3, new ItemStack(Items.STONE, 8));
            container.setItem(5, new ItemStack(Items.GLASS, 8));
            ContainerItemStorage storage = new ContainerItemStorage(container);

            Set<IItemKey> inScope = new HashSet<>();
            for (var view : ProviderModule.croppedView(storage, mode).contents()) {
                inScope.add(view.resource());
            }

            assertThat(advertised(storage, mode).keySet())
                    .as("%s advertises exactly the cropped view's slots", mode)
                    .isEqualTo(inScope);

            Set<IItemKey> extractable = new HashSet<>();
            for (IItemKey key : List.of(COBBLE, DIRT, STONE, GLASS)) {
                if (module().extractItems(storage, key, 8, mode, true) > 0) extractable.add(key);
            }
            assertThat(extractable)
                    .as("%s extracts from exactly the cropped view's slots", mode)
                    .isEqualTo(inScope);
        }
    }

    /** What the supply scan would publish for this storage under this mode. */
    private static Map<IItemKey, Long> advertised(ContainerItemStorage storage, ProviderMode mode) {
        PipeContext ctx = new PipeContext(null, BlockPos.ZERO, null, new FakePipeAccess());
        Map<IItemKey, Long> amounts = new HashMap<>();
        module().scanStorage(ctx, storage, mode, new HashMap<>(), amounts);
        return amounts;
    }

    /** A single pool of one item with no slot identity — the shape the crop has to decline. */
    private static final class ResourceScopedStorage implements IItemStorage {
        private long amount;

        private ResourceScopedStorage(long amount) {
            this.amount = amount;
        }

        @Override
        public long insert(IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0 || !item.equals(COBBLE)) return 0;
            if (!simulate) amount += maxAmount;
            return maxAmount;
        }

        @Override
        public long extract(IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0 || !item.equals(COBBLE)) return 0;
            long taken = Math.min(maxAmount, amount);
            if (!simulate) amount -= taken;
            return taken;
        }

        @Override
        public Iterable<IItemView> contents() {
            if (amount <= 0) return List.of();
            return List.of(new IItemView() {
                @Override public IItemKey resource() { return COBBLE; }
                @Override public long amount() { return amount; }
            });
        }
    }

    /** Exposes only the named slots to every face, waving both directional checks through. */
    private static final class FacedContainer extends SimpleContainer implements WorldlyContainer {
        private final int[] face;

        private FacedContainer(int size, int... face) {
            super(size);
            this.face = face;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return face;
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
}
