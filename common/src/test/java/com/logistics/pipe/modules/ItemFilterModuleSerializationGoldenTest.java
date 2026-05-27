package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Golden-fixture regression tests for {@link ItemFilterModule} serialization.
 *
 * <p>Each test injects a hardcoded NBT blob that represents the save format from a known
 * stable release, then reads it back through the current module code. If the module's
 * serialization format changes (key renames, structural changes, etc.) these tests will
 * fail immediately — before the change ships and corrupts player saves.
 *
 * <p>This is the direct lesson from the 0.5.4 release: filter data was silently lost when
 * the save format changed without a migration path. Adding this test before that release
 * would have caught the regression.
 *
 * <h2>How to add a new golden fixture</h2>
 * Before each new release, capture a snapshot of the current format by running a test world,
 * setting filters on a diamond pipe, and recording what {@link FakePipeAccess#getRawState}
 * returns. Commit that snapshot as a new fixture method here. Future releases must be able
 * to load it.
 */
@DisplayName("ItemFilterModule — golden serialization fixtures")
class ItemFilterModuleSerializationGoldenTest {

    // The module state key used by ItemFilterModule (getStateKey() = simpleName().toLowerCase())
    // DO NOT change this string — if ItemFilterModule is renamed, it must keep returning "itemfiltermodule"
    private static final String MODULE_STATE_KEY = "itemfiltermodule";

    private ItemFilterModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ItemFilterModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ---------------------------------------------------------------------------
    // 0.5.5 format: filters stored as CompoundTag keyed by direction name,
    //               each direction holds a ListTag of 8 StringTag item IDs.
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("0.5.5 format: loads single configured direction correctly")
    void golden_0_5_5_singleDirectionWithFilters() {
        // Inject NBT in the exact format written by 0.5.5
        injectFilters(filtersWithNorth("minecraft:diamond", "minecraft:emerald"));

        List<String> slots = module.getFilterSlots(ctx, Direction.NORTH);

        assertThat(slots).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(slots.get(0)).isEqualTo("minecraft:diamond");
        assertThat(slots.get(1)).isEqualTo("minecraft:emerald");
        assertThat(slots.get(2)).isEmpty();
    }

    @Test
    @DisplayName("0.5.5 format: unconfigured directions return empty slots (no silent data corruption)")
    void golden_0_5_5_unconfiguredDirectionIsEmpty() {
        injectFilters(filtersWithNorth("minecraft:diamond"));

        // SOUTH was not in the saved data — must return empty, not throw
        List<String> south = module.getFilterSlots(ctx, Direction.SOUTH);
        assertThat(south).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(south).allMatch(String::isEmpty);
    }

    @Test
    @DisplayName("0.5.5 format: all six directions load independently")
    void golden_0_5_5_allSixDirections() {
        CompoundTag filters = new CompoundTag();
        filters.put("north",  singleItemList("minecraft:diamond"));
        filters.put("south",  singleItemList("minecraft:emerald"));
        filters.put("east",   singleItemList("minecraft:iron_ingot"));
        filters.put("west",   singleItemList("minecraft:gold_ingot"));
        filters.put("up",     singleItemList("minecraft:copper_ingot"));
        filters.put("down",   singleItemList("minecraft:iron_nugget"));
        injectFilters(filters);

        assertThat(module.getFilterSlots(ctx, Direction.NORTH).get(0)).isEqualTo("minecraft:diamond");
        assertThat(module.getFilterSlots(ctx, Direction.SOUTH).get(0)).isEqualTo("minecraft:emerald");
        assertThat(module.getFilterSlots(ctx, Direction.EAST).get(0)).isEqualTo("minecraft:iron_ingot");
        assertThat(module.getFilterSlots(ctx, Direction.WEST).get(0)).isEqualTo("minecraft:gold_ingot");
        assertThat(module.getFilterSlots(ctx, Direction.UP).get(0)).isEqualTo("minecraft:copper_ingot");
        assertThat(module.getFilterSlots(ctx, Direction.DOWN).get(0)).isEqualTo("minecraft:iron_nugget");
    }

    @Test
    @DisplayName("0.5.5 format: partially-filled list (fewer than 8 items) pads with empty strings")
    void golden_0_5_5_shortListPadsWithEmpty() {
        // A save that only stored 3 items in a filter slot (could happen if the slot count ever changes)
        ListTag shortList = new ListTag();
        shortList.add(StringTag.valueOf("minecraft:diamond"));
        shortList.add(StringTag.valueOf("minecraft:coal"));
        shortList.add(StringTag.valueOf(""));
        CompoundTag filters = new CompoundTag();
        filters.put("north", shortList);
        injectFilters(filters);

        List<String> slots = module.getFilterSlots(ctx, Direction.NORTH);
        assertThat(slots).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(slots.get(0)).isEqualTo("minecraft:diamond");
        assertThat(slots.get(1)).isEqualTo("minecraft:coal");
        assertThat(slots.get(2)).isEmpty();
        // Indices beyond the saved list are padded with empty
        assertThat(slots.get(3)).isEmpty();
        assertThat(slots.get(7)).isEmpty();
    }

    @Test
    @DisplayName("state key is stable — module rename would break existing saves")
    void moduleStateKeyIsStable() {
        // This test intentionally checks the key string. If ItemFilterModule is ever
        // renamed, getStateKey() MUST be overridden to keep returning "itemfiltermodule".
        assertThat(module.getStateKey()).isEqualTo(MODULE_STATE_KEY);
    }

    // ---------------------------------------------------------------------------
    // Backward-compat: getFilterStacks() must read old StringTag format correctly.
    // (New CompoundTag round-trip tests require RegistryAccess and run in-game.)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getFilterStacks: old StringTag format returns item-only stacks (no NPE)")
    void getFilterStacks_readsOldStringFormat() {
        injectFilters(filtersWithNorth("minecraft:diamond"));

        // getFilterStacks() with null world falls back to item-ID only for old format
        List<ItemStack> stacks = module.getFilterStacks(ctx, Direction.NORTH);

        assertThat(stacks).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        // First slot is non-empty (item loaded by ID)
        assertThat(stacks.get(0).isEmpty()).isFalse();
        // Component-less (components patch is empty for a plain stack)
        assertThat(stacks.get(0).getComponentsPatch().isEmpty()).isTrue();
        // Remaining slots are empty
        for (int i = 1; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
            assertThat(stacks.get(i).isEmpty()).isTrue();
        }
    }

    @Test
    @DisplayName("getFilterStacks: missing direction returns all-empty list (no NPE)")
    void getFilterStacks_missingDirectionIsAllEmpty() {
        injectFilters(filtersWithNorth("minecraft:diamond"));

        List<ItemStack> stacks = module.getFilterStacks(ctx, Direction.SOUTH);

        assertThat(stacks).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(stacks).allMatch(ItemStack::isEmpty);
    }

    @Test
    @DisplayName("setFilterStacks with null world falls back gracefully (no NPE)")
    void setFilterStacks_nullWorldIsNoop() {
        List<ItemStack> toSet = Collections.nCopies(ItemFilterModule.FILTER_SLOTS_PER_SIDE, ItemStack.EMPTY);
        assertDoesNotThrow(() -> module.setFilterStacks(ctx, Direction.NORTH, toSet));
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Inject a filters CompoundTag directly into the fake pipe's module state. */
    private void injectFilters(CompoundTag filters) {
        access.getRawState(MODULE_STATE_KEY).put("filters", filters);
    }

    /** Build a filters CompoundTag with only north configured. */
    private CompoundTag filtersWithNorth(String... items) {
        CompoundTag filters = new CompoundTag();
        filters.put("north", itemList(items));
        return filters;
    }

    /** Build a padded ListTag from the given item IDs (padded to FILTER_SLOTS_PER_SIDE). */
    private ListTag itemList(String... items) {
        ListTag list = new ListTag();
        for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
            String value = (i < items.length) ? items[i] : "";
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    /** Build a padded ListTag with one item ID and the rest empty. */
    private ListTag singleItemList(String itemId) {
        return itemList(itemId);
    }
}
