package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProcessModule")
class ProcessModuleTest {

    private ProcessModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ProcessModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("KEY_INPUTS is 'inputs'")
    void keyInputs_value() {
        assertThat(ProcessModule.KEY_INPUTS).isEqualTo("inputs");
    }

    @Test
    @DisplayName("KEY_OUTPUTS is 'outputs'")
    void keyOutputs_value() {
        assertThat(ProcessModule.KEY_OUTPUTS).isEqualTo("outputs");
    }

    @Test
    @DisplayName("ENTRY_ITEM is 'item'")
    void entryItem_value() {
        assertThat(ProcessModule.ENTRY_ITEM).isEqualTo("item");
    }

    @Test
    @DisplayName("ENTRY_COUNT is 'count'")
    void entryCount_value() {
        assertThat(ProcessModule.ENTRY_COUNT).isEqualTo("count");
    }

    @Test
    @DisplayName("ENTRY_DEST is 'dest'")
    void entryDest_value() {
        assertThat(ProcessModule.ENTRY_DEST).isEqualTo("dest");
    }

    // ==================== isActive ====================

    @Test
    @DisplayName("isActive returns false when no job is present")
    void isActive_falseByDefault() {
        assertThat(module.isActive(ctx)).isFalse();
    }

    // ==================== Input config — defaults ====================

    @Test
    @DisplayName("getInputItem returns empty string for unconfigured slot")
    void getInputItem_emptyByDefault() {
        assertThat(module.getInputItem(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("getInputCount returns 1 for unconfigured slot")
    void getInputCount_defaultsToOne() {
        assertThat(module.getInputCount(ctx, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("getInputDest returns empty string for unconfigured slot")
    void getInputDest_emptyByDefault() {
        assertThat(module.getInputDest(ctx, 0)).isEmpty();
    }

    // ==================== Input config — write/read round-trip ====================

    @Test
    @DisplayName("setInput persists itemId, count, and dest — readable back")
    void setInput_persistsAndReadsBack() {
        module.setInput(ctx, 0, "minecraft:iron_ore", 2, "furnace_input");

        assertThat(module.getInputItem(ctx, 0)).isEqualTo("minecraft:iron_ore");
        assertThat(module.getInputCount(ctx, 0)).isEqualTo(2);
        assertThat(module.getInputDest(ctx, 0)).isEqualTo("furnace_input");
    }

    @Test
    @DisplayName("setInput with empty dest stores empty dest")
    void setInput_emptyDest_storesEmpty() {
        module.setInput(ctx, 0, "minecraft:iron_ore", 1, "");

        assertThat(module.getInputDest(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("setInput clamps count to minimum 1")
    void setInput_clampsCountToOne() {
        module.setInput(ctx, 0, "minecraft:iron_ore", 0, "");
        assertThat(module.getInputCount(ctx, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("setInput clears slot when itemId is empty")
    void setInput_clearSlotWithEmptyId() {
        module.setInput(ctx, 0, "minecraft:iron_ore", 1, "");
        module.setInput(ctx, 0, "", 1, "");

        assertThat(module.getInputItem(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("multiple input slots are independent")
    void setInput_multipleSlotsAreIndependent() {
        module.setInput(ctx, 0, "minecraft:iron_ore", 1, "");
        module.setInput(ctx, 2, "minecraft:coal", 3, "furnace");

        assertThat(module.getInputItem(ctx, 0)).isEqualTo("minecraft:iron_ore");
        assertThat(module.getInputItem(ctx, 2)).isEqualTo("minecraft:coal");
        assertThat(module.getInputCount(ctx, 2)).isEqualTo(3);
    }

    @Test
    @DisplayName("setInput silently ignores out-of-bounds slot")
    void setInput_ignoresOutOfBoundsSlot() {
        fillEveryInputSlot();

        module.setInput(ctx, -1, "minecraft:iron_ore", 1, "stray");
        module.setInput(ctx, ProcessModule.MAX_INPUTS, "minecraft:iron_ore", 1, "stray");

        // "Ignored" has to mean the configuration is untouched — clamping into slot 0, or writing
        // to a neighbouring valid slot, would silently rewrite what the player set up.
        assertEveryInputSlotStillHoldsItsFiller();
    }

    /** Distinct marker per slot so a stray write is attributable, and none of them is the probe item. */
    private static String filler(int slot) {
        return "minecraft:filler_" + slot;
    }

    private void fillEveryInputSlot() {
        for (int slot = 0; slot < ProcessModule.MAX_INPUTS; slot++) {
            module.setInput(ctx, slot, filler(slot), slot + 1, "dest_" + slot);
        }
    }

    private void assertEveryInputSlotStillHoldsItsFiller() {
        for (int slot = 0; slot < ProcessModule.MAX_INPUTS; slot++) {
            assertThat(module.getInputItem(ctx, slot)).as("input slot %d", slot).isEqualTo(filler(slot));
            assertThat(module.getInputCount(ctx, slot)).as("input count %d", slot).isEqualTo(slot + 1);
            assertThat(module.getInputDest(ctx, slot)).as("input dest %d", slot).isEqualTo("dest_" + slot);
        }
    }

    // ==================== Output config — defaults ====================

    @Test
    @DisplayName("getOutputItem returns empty string for unconfigured slot")
    void getOutputItem_emptyByDefault() {
        assertThat(module.getOutputItem(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("getOutputCount returns 1 for unconfigured slot")
    void getOutputCount_defaultsToOne() {
        assertThat(module.getOutputCount(ctx, 0)).isEqualTo(1);
    }

    // ==================== Output config — write/read round-trip ====================

    @Test
    @DisplayName("setOutput persists itemId and count — readable back")
    void setOutput_persistsAndReadsBack() {
        module.setOutput(ctx, 0, "minecraft:iron_ingot", 2);

        assertThat(module.getOutputItem(ctx, 0)).isEqualTo("minecraft:iron_ingot");
        assertThat(module.getOutputCount(ctx, 0)).isEqualTo(2);
    }

    @Test
    @DisplayName("setOutput clamps count to minimum 1")
    void setOutput_clampsCountToOne() {
        module.setOutput(ctx, 0, "minecraft:iron_ingot", 0);
        assertThat(module.getOutputCount(ctx, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("setOutput clears slot when itemId is empty")
    void setOutput_clearSlotWithEmptyId() {
        module.setOutput(ctx, 0, "minecraft:iron_ingot", 1);
        module.setOutput(ctx, 0, "", 1);

        assertThat(module.getOutputItem(ctx, 0)).isEmpty();
    }

    @Test
    @DisplayName("setOutput silently ignores out-of-bounds slot")
    void setOutput_ignoresOutOfBoundsSlot() {
        module.setOutput(ctx, 0, "minecraft:gold_ingot", 7);

        module.setOutput(ctx, -1, "minecraft:iron_ingot", 1);
        module.setOutput(ctx, ProcessModule.MAX_OUTPUTS, "minecraft:iron_ingot", 1);

        // The only valid output slot must still hold what the player configured; a clamp would
        // have overwritten it with the probe item.
        assertThat(module.getOutputItem(ctx, 0)).isEqualTo("minecraft:gold_ingot");
        assertThat(module.getOutputCount(ctx, 0)).isEqualTo(7);
    }

    // ==================== Input satellite ID ====================

    @Test
    @DisplayName("getInputSatelliteId returns 0 by default")
    void getInputSatelliteId_defaultsToZero() {
        assertThat(module.getInputSatelliteId(ctx)).isEqualTo(0);
    }

    @Test
    @DisplayName("setInputSatelliteId persists and is readable back")
    void setInputSatelliteId_persistsAndReadsBack() {
        module.setInputSatelliteId(ctx, 3);
        assertThat(module.getInputSatelliteId(ctx)).isEqualTo(3);
    }

    @Test
    @DisplayName("setInputSatelliteId clamps negative values to 0")
    void setInputSatelliteId_clampsNegativeToZero() {
        module.setInputSatelliteId(ctx, -1);
        assertThat(module.getInputSatelliteId(ctx)).isEqualTo(0);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments scan counter for first 19 ticks")
    void onTick_incrementsScanCounter_belowInterval() {
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_scan", 0);
        assertThat(ticks).isEqualTo(5);
    }
}
