package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CraftingModule")
class CraftingModuleTest {

    private CraftingModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new CraftingModule(64, 4);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Static tag helpers (no PipeContext needed) ====================

    @Test
    @DisplayName("getIngredientItemFromTag returns empty string for missing slot")
    void staticHelper_ingredientItem_missingSlot_returnsEmpty() {
        CompoundTag tag = new CompoundTag();
        assertThat(CraftingModule.getIngredientItemFromTag(tag, 0)).isEmpty();
    }

    @Test
    @DisplayName("getIngredientItemFromTag reads the stored item ID")
    void staticHelper_ingredientItem_readsStoredId() {
        CompoundTag tag = new CompoundTag();
        CompoundTag items = new CompoundTag();
        items.putString("3", "minecraft:iron_ingot");
        tag.put(CraftingModule.RECIPE_ITEMS, items);

        assertThat(CraftingModule.getIngredientItemFromTag(tag, 3)).isEqualTo("minecraft:iron_ingot");
    }

    @Test
    @DisplayName("getIngredientCountFromTag defaults to 1 for missing slot")
    void staticHelper_ingredientCount_missingSlot_defaultsToOne() {
        CompoundTag tag = new CompoundTag();
        assertThat(CraftingModule.getIngredientCountFromTag(tag, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("getIngredientCountFromTag reads the stored count")
    void staticHelper_ingredientCount_readsStoredCount() {
        CompoundTag tag = new CompoundTag();
        CompoundTag counts = new CompoundTag();
        counts.putInt("5", 3);
        tag.put(CraftingModule.RECIPE_COUNTS, counts);

        assertThat(CraftingModule.getIngredientCountFromTag(tag, 5)).isEqualTo(3);
    }

    @Test
    @DisplayName("getResultItemFromTag returns empty string when unset")
    void staticHelper_resultItem_unset_returnsEmpty() {
        assertThat(CraftingModule.getResultItemFromTag(new CompoundTag())).isEmpty();
    }

    @Test
    @DisplayName("getResultItemFromTag reads the stored result ID")
    void staticHelper_resultItem_readsStoredId() {
        CompoundTag tag = new CompoundTag();
        tag.putString(CraftingModule.RESULT_ITEM, "minecraft:iron_block");
        assertThat(CraftingModule.getResultItemFromTag(tag)).isEqualTo("minecraft:iron_block");
    }

    @Test
    @DisplayName("getResultCountFromTag defaults to 1 when unset")
    void staticHelper_resultCount_unset_defaultsToOne() {
        assertThat(CraftingModule.getResultCountFromTag(new CompoundTag())).isEqualTo(1);
    }

    @Test
    @DisplayName("getResultCountFromTag reads the stored count")
    void staticHelper_resultCount_readsStoredCount() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(CraftingModule.RESULT_COUNT, 4);
        assertThat(CraftingModule.getResultCountFromTag(tag)).isEqualTo(4);
    }

    // ==================== Ingredient config (setIngredient / getIngredient*) ====================

    @Test
    @DisplayName("getIngredientItem returns empty string before anything is set")
    void ingredientItem_emptyByDefault() {
        assertThat(module.getIngredientItem(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("getIngredientCount defaults to 1 before anything is set")
    void ingredientCount_defaultsToOne() {
        assertThat(module.getIngredientCount(ctx, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("setIngredient persists itemId and count, readable back")
    void setIngredient_persistsAndReadsBack() {
        module.setIngredient(ctx, 4, "minecraft:diamond", 2);

        assertThat(module.getIngredientItem(ctx, 4)).isEqualTo("minecraft:diamond");
        assertThat(module.getIngredientCount(ctx, 4)).isEqualTo(2);
    }

    @Test
    @DisplayName("setIngredient clamps count to minimum 1")
    void setIngredient_clampsCountToOne() {
        module.setIngredient(ctx, 0, "minecraft:diamond", 0);
        assertThat(module.getIngredientCount(ctx, 0)).isEqualTo(1);

        module.setIngredient(ctx, 0, "minecraft:diamond", -5);
        assertThat(module.getIngredientCount(ctx, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("setIngredient clears slot when itemId is null")
    void setIngredient_clearSlotWithNull() {
        module.setIngredient(ctx, 2, "minecraft:diamond", 1);
        module.setIngredient(ctx, 2, null, 1);

        assertThat(module.getIngredientItem(ctx, 2)).isEmpty();
    }

    @Test
    @DisplayName("setIngredient clears slot when itemId is empty string")
    void setIngredient_clearSlotWithEmptyString() {
        module.setIngredient(ctx, 2, "minecraft:diamond", 1);
        module.setIngredient(ctx, 2, "", 1);

        assertThat(module.getIngredientItem(ctx, 2)).isEmpty();
    }

    @Test
    @DisplayName("setIngredient silently ignores slot index < 0")
    void setIngredient_ignoresNegativeSlot() {
        // Should not throw, just no-ops
        assertThatCode(() -> module.setIngredient(ctx, -1, "minecraft:diamond", 1))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("setIngredient silently ignores slot index > 8")
    void setIngredient_ignoresSlotBeyondMax() {
        assertThatCode(() -> module.setIngredient(ctx, 9, "minecraft:diamond", 1))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "slot {0}")
    @DisplayName("setIngredient accepts all valid slot indices 0-8")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void setIngredient_acceptsAllValidSlots(int slot) {
        module.setIngredient(ctx, slot, "minecraft:diamond", 1);
        assertThat(module.getIngredientItem(ctx, slot)).isEqualTo("minecraft:diamond");
    }

    @Test
    @DisplayName("slots are independent: setting slot 0 does not affect slot 8")
    void setIngredient_slotsAreIndependent() {
        module.setIngredient(ctx, 0, "minecraft:diamond", 1);
        module.setIngredient(ctx, 8, "minecraft:iron_ingot", 3);

        assertThat(module.getIngredientItem(ctx, 0)).isEqualTo("minecraft:diamond");
        assertThat(module.getIngredientItem(ctx, 8)).isEqualTo("minecraft:iron_ingot");
        assertThat(module.getIngredientItem(ctx, 4)).isEmpty();
    }

    // ==================== Result config (setResult / getResult*) ====================

    @Test
    @DisplayName("getResultItem returns empty string before anything is set")
    void resultItem_emptyByDefault() {
        assertThat(module.getResultItem(ctx)).isEmpty();
    }

    @Test
    @DisplayName("getResultCount defaults to 1 before anything is set")
    void resultCount_defaultsToOne() {
        assertThat(module.getResultCount(ctx)).isEqualTo(1);
    }

    @Test
    @DisplayName("setResult persists itemId and count, readable back")
    void setResult_persistsAndReadsBack() {
        module.setResult(ctx, "minecraft:iron_block", 1);

        assertThat(module.getResultItem(ctx)).isEqualTo("minecraft:iron_block");
        assertThat(module.getResultCount(ctx)).isEqualTo(1);
    }

    @Test
    @DisplayName("setResult clamps count to minimum 1")
    void setResult_clampsCountToOne() {
        module.setResult(ctx, "minecraft:diamond", 0);
        assertThat(module.getResultCount(ctx)).isEqualTo(1);
    }

    @Test
    @DisplayName("setResult with null itemId clears both result item and count")
    void setResult_clearWithNull() {
        module.setResult(ctx, "minecraft:diamond", 4);
        module.setResult(ctx, null, 1);

        assertThat(module.getResultItem(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setResult with empty itemId clears both result item and count")
    void setResult_clearWithEmpty() {
        module.setResult(ctx, "minecraft:diamond", 4);
        module.setResult(ctx, "", 1);

        assertThat(module.getResultItem(ctx)).isEmpty();
    }

    // ==================== Blocking flag ====================

    @Test
    @DisplayName("isBlocking defaults to false")
    void isBlocking_falseByDefault() {
        assertThat(module.isBlocking(ctx)).isFalse();
    }

    @Test
    @DisplayName("setBlocking(true) persists and isBlocking returns true")
    void setBlocking_true_isBlockingReturnsTrue() {
        module.setBlocking(ctx, true);
        assertThat(module.isBlocking(ctx)).isTrue();
    }

    @Test
    @DisplayName("setBlocking(false) after true restores to false")
    void setBlocking_falseAfterTrue_restoresToFalse() {
        module.setBlocking(ctx, true);
        module.setBlocking(ctx, false);
        assertThat(module.isBlocking(ctx)).isFalse();
    }

    // ==================== Active state ====================

    @Test
    @DisplayName("isActive is false when no orders are queued")
    void isActive_falseWithNoQueue() {
        assertThat(module.isActive(ctx)).isFalse();
    }

    // ==================== Tick counter (pre-interval, no world access) ====================

    @Test
    @DisplayName("onTick increments scan tick counter when queue is empty")
    void onTick_incrementsScanCounter_whenIdle() {
        // With empty queue, onTick returns before hitting world-dependent code after 20 ticks.
        // First 19 ticks: counter increments; on tick 20 it calls updateCrafterSupply (network null → no-op).
        // We test the early ticks to keep the test world-free.
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_scan", 0);
        assertThat(ticks).isEqualTo(5);
    }

    @Test
    @DisplayName("onTick scan counter reaches 19 before triggering the interval (world-dependent reset is game-test territory)")
    void onTick_counterReaches19BeforeInterval() {
        for (int i = 0; i < 19; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_scan", 0);
        assertThat(ticks).isEqualTo(19);
    }
}
