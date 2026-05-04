package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SupplierModule")
class SupplierModuleTest {

    private SupplierModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new SupplierModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== SupplyMode enum ====================

    @Test
    @DisplayName("SupplyMode.BULK50 is ordinal 0")
    void supplyMode_bulk50_ordinalZero() {
        assertThat(SupplierModule.SupplyMode.BULK50.ordinal()).isEqualTo(0);
    }

    @Test
    @DisplayName("SupplyMode.INFINITE is ordinal 1")
    void supplyMode_infinite_ordinalOne() {
        assertThat(SupplierModule.SupplyMode.INFINITE.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("SupplyMode.PARTIAL is ordinal 2")
    void supplyMode_partial_ordinalTwo() {
        assertThat(SupplierModule.SupplyMode.PARTIAL.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("SupplyMode.FULL is ordinal 3")
    void supplyMode_full_ordinalThree() {
        assertThat(SupplierModule.SupplyMode.FULL.ordinal()).isEqualTo(3);
    }

    @Test
    @DisplayName("SupplyMode.BULK100 is ordinal 4")
    void supplyMode_bulk100_ordinalFour() {
        assertThat(SupplierModule.SupplyMode.BULK100.ordinal()).isEqualTo(4);
    }

    @Test
    @DisplayName("SupplyMode has exactly 5 values")
    void supplyMode_hasFiveValues() {
        assertThat(SupplierModule.SupplyMode.values()).hasSize(5);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("MAX_SUPPLY_SLOTS is 9")
    void maxSupplySlots_isNine() {
        assertThat(SupplierModule.MAX_SUPPLY_SLOTS).isEqualTo(9);
    }

    // ==================== Mode read/write ====================

    @Test
    @DisplayName("getMode defaults to PARTIAL")
    void getMode_defaultsToPartial() {
        assertThat(module.getMode(ctx)).isEqualTo(SupplierModule.SupplyMode.PARTIAL);
    }

    @Test
    @DisplayName("setMode/getMode round-trip for each mode")
    void setMode_getMode_roundTrip() {
        for (SupplierModule.SupplyMode mode : SupplierModule.SupplyMode.values()) {
            module.setMode(ctx, mode);
            assertThat(module.getMode(ctx)).isEqualTo(mode);
        }
    }

    @Test
    @DisplayName("getModeOrdinal returns PARTIAL ordinal (2) by default")
    void getModeOrdinal_defaultsToPartial() {
        assertThat(module.getModeOrdinal(ctx)).isEqualTo(SupplierModule.SupplyMode.PARTIAL.ordinal());
    }

    @ParameterizedTest(name = "ordinal {0}")
    @DisplayName("setModeFromOrdinal accepts all valid ordinals 0-4")
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void setModeFromOrdinal_validOrdinals(int ordinal) {
        module.setModeFromOrdinal(ctx, ordinal);
        assertThat(module.getModeOrdinal(ctx)).isEqualTo(ordinal);
    }

    @Test
    @DisplayName("setModeFromOrdinal throws for negative ordinal")
    void setModeFromOrdinal_rejectsNegativeOrdinal() {
        assertThatThrownBy(() -> module.setModeFromOrdinal(ctx, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setModeFromOrdinal throws for ordinal >= mode count")
    void setModeFromOrdinal_rejectsOrdinalAtOrBeyondMax() {
        assertThatThrownBy(() -> module.setModeFromOrdinal(ctx, SupplierModule.SupplyMode.values().length))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Supply config read/write ====================

    @Test
    @DisplayName("getSupplyConfigs returns empty list when nothing is configured")
    void getSupplyConfigs_emptyByDefault() {
        assertThat(module.getSupplyConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setSupplyConfig persists item ID and amount, readable back")
    void setSupplyConfig_persistsAndReadsBack() {
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 16);

        List<SupplierModule.SupplyConfig> configs = module.getSupplyConfigs(ctx);
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).itemId()).isEqualTo("minecraft:diamond");
        assertThat(configs.get(0).amount()).isEqualTo(16);
    }

    @Test
    @DisplayName("setSupplyConfig in multiple slots are independent")
    void setSupplyConfig_multipleSlotsAreIndependent() {
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 8);
        module.setSupplyConfig(ctx, 3, "minecraft:iron_ingot", 64);

        List<SupplierModule.SupplyConfig> configs = module.getSupplyConfigs(ctx);
        assertThat(configs).hasSize(2);
        assertThat(configs).anyMatch(c -> c.itemId().equals("minecraft:diamond") && c.amount() == 8);
        assertThat(configs).anyMatch(c -> c.itemId().equals("minecraft:iron_ingot") && c.amount() == 64);
    }

    @Test
    @DisplayName("setSupplyConfig clears a slot when amount is 0")
    void setSupplyConfig_clearSlotWithZeroAmount() {
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 16);
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 0);

        assertThat(module.getSupplyConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setSupplyConfig clears a slot when itemId is empty")
    void setSupplyConfig_clearSlotWithEmptyId() {
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 16);
        module.setSupplyConfig(ctx, 0, "", 16);

        assertThat(module.getSupplyConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setSupplyConfig throws for negative slot index")
    void setSupplyConfig_rejectsNegativeSlot() {
        assertThatThrownBy(() -> module.setSupplyConfig(ctx, -1, "minecraft:diamond", 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setSupplyConfig throws for slot >= MAX_SUPPLY_SLOTS")
    void setSupplyConfig_rejectsSlotAtOrBeyondMax() {
        assertThatThrownBy(() -> module.setSupplyConfig(ctx, SupplierModule.MAX_SUPPLY_SLOTS, "minecraft:diamond", 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments counter for first 19 ticks")
    void onTick_incrementsCounter_belowInterval() {
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_check", 0);
        assertThat(ticks).isEqualTo(5);
    }
}
