package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ItemStoreComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    // Slot 0 = input, slots 1 and 2 = outputs.
    private static final SlotRole[] ROLES = {SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT};

    private int changes;

    private ItemStoreComponent store() {
        return new ItemStoreComponent(
                "items", ROLES, SidedLayout.furnace(new int[] {0}, new int[] {1, 2}, s -> true), () -> changes++);
    }

    @Test
    void exposesInputSlotAndConsumesIt() {
        ItemStoreComponent store = store();
        store.container().setItem(0, new ItemStack(Items.RAW_IRON, 4));

        assertThat(store.input().getItem()).isEqualTo(Items.RAW_IRON);
        assertThat(store.input().getCount()).isEqualTo(4);

        store.consumeInput(3);
        assertThat(store.input().getCount()).isEqualTo(1);
        assertThat(changes).isPositive();
    }

    @Test
    void outputCountReflectsDeclaredOutputSlots() {
        assertThat(store().outputCount()).isEqualTo(2);
    }

    @Test
    void acceptsIntoEmptyOrMatchingOutput() {
        ItemStoreComponent store = store();

        // Empty output accepts anything.
        assertThat(store.canAcceptInto(0, new ItemStack(Items.IRON_INGOT, 1))).isTrue();

        store.produceInto(0, new ItemStack(Items.IRON_INGOT, 60));
        // Same item with room to merge.
        assertThat(store.canAcceptInto(0, new ItemStack(Items.IRON_INGOT, 4))).isTrue();
        // Same item but would overflow the 64 stack.
        assertThat(store.canAcceptInto(0, new ItemStack(Items.IRON_INGOT, 8))).isFalse();
        // A different item never merges.
        assertThat(store.canAcceptInto(0, new ItemStack(Items.GOLD_INGOT, 1))).isFalse();
    }

    @Test
    void canAcceptRejectsOutOfRangeIndex() {
        ItemStoreComponent store = store();
        assertThat(store.canAcceptInto(2, new ItemStack(Items.IRON_INGOT))).isFalse();
        assertThat(store.canAcceptInto(-1, new ItemStack(Items.IRON_INGOT))).isFalse();
    }

    @Test
    void produceMergesIntoMatchingStack() {
        ItemStoreComponent store = store();
        store.produceInto(1, new ItemStack(Items.GUNPOWDER, 10));
        store.produceInto(1, new ItemStack(Items.GUNPOWDER, 5));

        // Output index 1 maps to the third inventory slot (the second OUTPUT role).
        assertThat(store.container().getItem(2).getCount()).isEqualTo(15);
    }

    @Test
    void produceCapsAtMaxStackSizeAndDropsTheOverflow() {
        ItemStoreComponent store = store();
        store.produceInto(0, new ItemStack(Items.IRON_INGOT, 60));
        store.produceInto(0, new ItemStack(Items.IRON_INGOT, 10)); // only 4 fit

        assertThat(store.container().getItem(1).getCount()).isEqualTo(64);
    }

    @Test
    void savesAndLoadsInventoryRoundTrip() {
        ItemStoreComponent writer = store();
        writer.container().setItem(0, new ItemStack(Items.RAW_IRON, 5));
        writer.container().setItem(1, new ItemStack(Items.IRON_INGOT, 2));

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);

        ItemStoreComponent reader = store();
        reader.load(tag, registries);

        assertThat(reader.container().getItem(0).getCount()).isEqualTo(5);
        assertThat(reader.container().getItem(1).getItem()).isEqualTo(Items.IRON_INGOT);
        assertThat(reader.container().getItem(1).getCount()).isEqualTo(2);
    }

    @Test
    void loadLegacyReadsTheOldInventoryRootKey() {
        ItemStoreComponent writer = store();
        writer.container().setItem(0, new ItemStack(Items.RAW_IRON, 7));

        // The component saves under "items"; the pre-component format keyed it as "Inventory" at the root.
        CompoundTag saved = new CompoundTag();
        writer.save(saved, registries);
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.put("Inventory", NbtCompat.getListOrEmpty(saved, "items"));

        ItemStoreComponent reader = store();
        reader.loadLegacy(legacyRoot, registries);

        assertThat(reader.container().getItem(0).getCount()).isEqualTo(7);
    }
}
