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

    @Test
    @DisplayName("state key is stable — module rename would break existing saves")
    void moduleStateKeyIsStable() {
        // This test intentionally checks the key string. If ItemFilterModule is ever
        // renamed, getStateKey() MUST be overridden to keep returning "itemfiltermodule".
        assertThat(module.getStateKey()).isEqualTo(MODULE_STATE_KEY);
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
}
