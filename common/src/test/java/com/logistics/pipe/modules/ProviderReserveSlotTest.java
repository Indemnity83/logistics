package com.logistics.pipe.modules;

import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.modules.ProviderModule.ProviderMode;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The crop modes name the slots they protect. Extraction has to drain the slots the crop selected,
 * not just the right total from wherever the storage happens to hand it over.
 */
@DisplayName("Provider crop modes protect the slots they name")
class ProviderReserveSlotTest extends MinecraftTestEnvironment {

    private static final IItemKey COBBLE = ItemStorageLookup.of(new ItemStack(Items.COBBLESTONE));

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
}
