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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Provider advertises only what it can extract. A machine's side face lists its input slot but
 * refuses to give it up; advertising it lets the network place orders the Provider cannot fill.
 */
@DisplayName("Provider advertises only extractable slots")
class ProviderExtractableSlotsTest extends MinecraftTestEnvironment {

    private static final IItemKey COBBLE = ItemStorageLookup.of(new ItemStack(Items.COBBLESTONE));
    private static final IItemKey DIAMOND = ItemStorageLookup.of(new ItemStack(Items.DIAMOND));

    private static ProviderModule module() {
        return new ProviderModule(64, 64);
    }

    @Test
    @DisplayName("an input slot the face refuses to extract is not advertised, in any mode")
    void inputSlot_isNeverAdvertised() {
        for (ProviderMode mode : ProviderMode.values()) {
            // Slot 1 is the machine's input; slots 0, 2 and 3 are its empty outputs. All face the pipe,
            // and the input sits mid-range so RESERVE and GUARDED do not crop it away on their own.
            MachineLikeContainer container = new MachineLikeContainer(4, 1);
            container.setItem(1, new ItemStack(Items.DIAMOND, 16));
            ContainerItemStorage storage = new ContainerItemStorage(container, Direction.NORTH);

            assertThat(advertised(storage, mode))
                    .as("%s must not offer the input slot's diamonds", mode)
                    .isEmpty();
            assertThat(module().extractItems(storage, DIAMOND, 16, mode, true)).isZero();
        }
    }

    @Test
    @DisplayName("an extractable slot beside a refused one is still advertised in full")
    void outputSlot_isStillAdvertised() {
        MachineLikeContainer container = new MachineLikeContainer(2, 0);
        container.setItem(0, new ItemStack(Items.DIAMOND, 16));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 8));
        ContainerItemStorage storage = new ContainerItemStorage(container, Direction.NORTH);

        assertThat(advertised(storage, ProviderMode.SUPPLY)).isEqualTo(Map.of(COBBLE, 8L));
    }

    @Test
    @DisplayName("Leave 1 Per Type keeps its one in an extractable slot, not the refused input")
    void sample_countsTheFirstExtractableSlotOfAType() {
        // The same item in the refused input slot and in an output slot.
        MachineLikeContainer container = new MachineLikeContainer(2, 0);
        container.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 8));
        ContainerItemStorage storage = new ContainerItemStorage(container, Direction.NORTH);

        assertThat(advertised(storage, ProviderMode.SAMPLE))
                .as("only slot 1 is supply, and it keeps the one sample")
                .isEqualTo(Map.of(COBBLE, 7L));
        assertThat(module().extractItems(storage, COBBLE, 64, ProviderMode.SAMPLE, false))
                .as("extraction honours the same sample as the advertisement")
                .isEqualTo(7);
        assertThat(container.getItem(0).getCount()).as("the input slot is untouched").isEqualTo(8);
        assertThat(container.getItem(1).getCount()).as("the sample stays behind").isEqualTo(1);
    }

    @Test
    @DisplayName("a resource-scoped storage that refuses extraction advertises nothing")
    void resourceScoped_refusingExtraction_isNotAdvertised() {
        IItemStorage storage = new InsertOnlyStorage(32);

        assertThat(advertised(storage, ProviderMode.SUPPLY)).isEmpty();
    }

    private static Map<IItemKey, Long> advertised(IItemStorage storage, ProviderMode mode) {
        PipeContext ctx = new PipeContext(null, BlockPos.ZERO, null, new FakePipeAccess());
        Map<IItemKey, Long> amounts = new HashMap<>();
        module().scanStorage(ctx, storage, mode, new HashMap<>(), amounts);
        return amounts;
    }

    /** Exposes every slot to every face, but refuses to extract the listed input slots. */
    private static final class MachineLikeContainer extends SimpleContainer implements WorldlyContainer {
        private final int[] all;
        private final int[] inputs;

        private MachineLikeContainer(int size, int... inputs) {
            super(size);
            this.all = new int[size];
            for (int i = 0; i < size; i++) all[i] = i;
            this.inputs = inputs;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return all;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            return isInput(slot);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return !isInput(slot);
        }

        private boolean isInput(int slot) {
            for (int s : inputs) {
                if (s == slot) return true;
            }
            return false;
        }
    }

    /** One pool of cobblestone with no slot identity that never gives any of it up. */
    private static final class InsertOnlyStorage implements IItemStorage {
        private final long amount;

        private InsertOnlyStorage(long amount) {
            this.amount = amount;
        }

        @Override
        public long insert(IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public long extract(IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public Iterable<IItemView> contents() {
            return List.of(new IItemView() {
                @Override public IItemKey resource() { return COBBLE; }
                @Override public long amount() { return amount; }
            });
        }
    }
}
