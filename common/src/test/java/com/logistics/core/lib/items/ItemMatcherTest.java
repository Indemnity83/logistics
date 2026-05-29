package com.logistics.core.lib.items;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemMatcher")
class ItemMatcherTest extends MinecraftTestEnvironment {

    // ==================== itemId ====================

    @Test
    @DisplayName("itemId returns registry ID string for a known item")
    void itemId_knownItem() {
        assertThat(ItemMatcher.itemId(new ItemStack(Items.DIAMOND))).isEqualTo("minecraft:diamond");
    }

    @Test
    @DisplayName("itemId returns minecraft:stone for stone")
    void itemId_stone() {
        assertThat(ItemMatcher.itemId(new ItemStack(Items.STONE))).isEqualTo("minecraft:stone");
    }

    // ==================== sameType ====================

    @Test
    @DisplayName("sameType returns true when both stacks share the same item")
    void sameType_sameItem_returnsTrue() {
        assertThat(ItemMatcher.sameType(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND))).isTrue();
    }

    @Test
    @DisplayName("sameType returns false when items differ")
    void sameType_differentItems_returnsFalse() {
        assertThat(ItemMatcher.sameType(new ItemStack(Items.DIAMOND), new ItemStack(Items.GOLD_INGOT))).isFalse();
    }

    @Test
    @DisplayName("sameType returns false when first stack is empty")
    void sameType_firstEmpty_returnsFalse() {
        assertThat(ItemMatcher.sameType(ItemStack.EMPTY, new ItemStack(Items.DIAMOND))).isFalse();
    }

    @Test
    @DisplayName("sameType returns false when second stack is empty")
    void sameType_secondEmpty_returnsFalse() {
        assertThat(ItemMatcher.sameType(new ItemStack(Items.DIAMOND), ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("sameType returns false when both stacks are empty")
    void sameType_bothEmpty_returnsFalse() {
        assertThat(ItemMatcher.sameType(ItemStack.EMPTY, ItemStack.EMPTY)).isFalse();
    }

    // ==================== sameItem ====================

    @Test
    @DisplayName("sameItem returns true for two identical plain stacks")
    void sameItem_identicalStacks_returnsTrue() {
        assertThat(ItemMatcher.sameItem(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND))).isTrue();
    }

    @Test
    @DisplayName("sameItem returns false when items differ")
    void sameItem_differentItems_returnsFalse() {
        assertThat(ItemMatcher.sameItem(new ItemStack(Items.DIAMOND), new ItemStack(Items.GOLD_INGOT))).isFalse();
    }

    @Test
    @DisplayName("sameItem returns false when first stack is empty")
    void sameItem_firstEmpty_returnsFalse() {
        assertThat(ItemMatcher.sameItem(ItemStack.EMPTY, new ItemStack(Items.DIAMOND))).isFalse();
    }

    @Test
    @DisplayName("sameItem returns false when second stack is empty")
    void sameItem_secondEmpty_returnsFalse() {
        assertThat(ItemMatcher.sameItem(new ItemStack(Items.DIAMOND), ItemStack.EMPTY)).isFalse();
    }
}
