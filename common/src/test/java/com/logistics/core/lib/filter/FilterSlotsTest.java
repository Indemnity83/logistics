package com.logistics.core.lib.filter;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FilterSlots filter modes")
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
    @DisplayName("empty filter: excluded always false")
    void emptyFilter_excluded_alwaysFalse() {
        assertThat(empty().excluded(new ItemStack(Items.DIAMOND))).isFalse();
    }

    @Test
    @DisplayName("empty filter: included always false")
    void emptyFilter_included_alwaysFalse() {
        assertThat(empty().included(new ItemStack(Items.DIAMOND))).isFalse();
    }

    // ==================== allowlist / include mode ====================

    @Test
    @DisplayName("allowlist: item in filter is allowed through")
    void allowList_matchingItem_notFiltered() {
        assertThat(withDiamond().excluded(new ItemStack(Items.DIAMOND))).isFalse();
    }

    @Test
    @DisplayName("allowlist: item not in filter is blocked")
    void allowList_nonMatchingItem_isFiltered() {
        assertThat(withDiamond().excluded(new ItemStack(Items.GOLD_INGOT))).isTrue();
    }

    // ==================== blocklist / exclude mode ====================

    @Test
    @DisplayName("blocklist: item in filter is blocked")
    void blockList_matchingItem_isFiltered() {
        assertThat(withDiamond().included(new ItemStack(Items.DIAMOND))).isTrue();
    }

    @Test
    @DisplayName("blocklist: item not in filter is allowed through")
    void blockList_nonMatchingItem_notFiltered() {
        assertThat(withDiamond().included(new ItemStack(Items.GOLD_INGOT))).isFalse();
    }

    // ==================== stale / unregistered IDs ====================

    @Test
    @DisplayName("stale item ID in filter is ignored — effectively empty filter")
    void staleId_treatedAsEmpty() {
        FilterSlots stale = empty().with(0, "unloaded_mod:ghost_item");
        assertThat(stale.excluded(new ItemStack(Items.DIAMOND))).isFalse();
    }
}
