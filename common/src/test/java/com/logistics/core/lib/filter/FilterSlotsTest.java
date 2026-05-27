package com.logistics.core.lib.filter;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FilterSlots.isFiltered")
class FilterSlotsTest extends MinecraftTestEnvironment {

    private static final int SIZE = 9;

    private FilterSlots empty() {
        return FilterSlots.load(new CompoundTag(), SIZE);
    }

    private FilterSlots withDiamond() {
        return empty().with(0, "minecraft:diamond");
    }

    // ==================== empty filter ====================

    @Test
    @DisplayName("empty filter never excludes any item (include mode)")
    void emptyFilter_includeMode_alwaysFalse() {
        assertThat(empty().isFiltered(new ItemStack(Items.DIAMOND), false)).isFalse();
    }

    @Test
    @DisplayName("empty filter never excludes any item (exclude mode)")
    void emptyFilter_excludeMode_alwaysFalse() {
        assertThat(empty().isFiltered(new ItemStack(Items.DIAMOND), true)).isFalse();
    }

    // ==================== include mode (inverted=false) ====================

    @Test
    @DisplayName("include mode: item in filter is not excluded")
    void includeMode_matchingItem_notFiltered() {
        assertThat(withDiamond().isFiltered(new ItemStack(Items.DIAMOND), false)).isFalse();
    }

    @Test
    @DisplayName("include mode: item not in filter is excluded")
    void includeMode_nonMatchingItem_isFiltered() {
        assertThat(withDiamond().isFiltered(new ItemStack(Items.GOLD_INGOT), false)).isTrue();
    }

    // ==================== exclude mode (inverted=true) ====================

    @Test
    @DisplayName("exclude mode: item in filter is excluded")
    void excludeMode_matchingItem_isFiltered() {
        assertThat(withDiamond().isFiltered(new ItemStack(Items.DIAMOND), true)).isTrue();
    }

    @Test
    @DisplayName("exclude mode: item not in filter is not excluded")
    void excludeMode_nonMatchingItem_notFiltered() {
        assertThat(withDiamond().isFiltered(new ItemStack(Items.GOLD_INGOT), true)).isFalse();
    }

    // ==================== stale / unregistered IDs ====================

    @Test
    @DisplayName("stale item ID in filter is ignored — effectively empty filter")
    void staleId_treatedAsEmpty() {
        FilterSlots stale = empty().with(0, "unloaded_mod:ghost_item");
        assertThat(stale.isFiltered(new ItemStack(Items.DIAMOND), false)).isFalse();
    }
}
