package com.logistics.pipe.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.logistics.pipe.modules.ProviderModule.ProviderMode.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("ProviderMode enum")
class ProviderModeTest {

    // ==================== Enum property contracts ====================

    @Test
    @DisplayName("SUPPLY: no hiding, no cropping")
    void supply_noHidingNoCropping() {
        assertThat(SUPPLY.isHideOnePerSlot()).isFalse();
        assertThat(SUPPLY.isHideOnePerType()).isFalse();
        assertThat(SUPPLY.getCropStart()).isZero();
        assertThat(SUPPLY.getCropEnd()).isZero();
    }

    @Test
    @DisplayName("RESERVE: skip first slot, no hiding per slot/type")
    void reserve_skipsFirstSlot() {
        assertThat(RESERVE.isHideOnePerSlot()).isFalse();
        assertThat(RESERVE.isHideOnePerType()).isFalse();
        assertThat(RESERVE.getCropStart()).isEqualTo(1);
        assertThat(RESERVE.getCropEnd()).isZero();
    }

    @Test
    @DisplayName("GUARDED: skip first and last slot, no hiding per slot/type")
    void guarded_skipsFirstAndLastSlot() {
        assertThat(GUARDED.isHideOnePerSlot()).isFalse();
        assertThat(GUARDED.isHideOnePerType()).isFalse();
        assertThat(GUARDED.getCropStart()).isEqualTo(1);
        assertThat(GUARDED.getCropEnd()).isEqualTo(1);
    }

    @Test
    @DisplayName("SEEDED: hide one per slot, no cropping")
    void seeded_hidesOnePerSlot() {
        assertThat(SEEDED.isHideOnePerSlot()).isTrue();
        assertThat(SEEDED.isHideOnePerType()).isFalse();
        assertThat(SEEDED.getCropStart()).isZero();
        assertThat(SEEDED.getCropEnd()).isZero();
    }

    @Test
    @DisplayName("SAMPLE: hide one per type, no cropping")
    void sample_hidesOnePerType() {
        assertThat(SAMPLE.isHideOnePerSlot()).isFalse();
        assertThat(SAMPLE.isHideOnePerType()).isTrue();
        assertThat(SAMPLE.getCropStart()).isZero();
        assertThat(SAMPLE.getCropEnd()).isZero();
    }

    // ==================== Ordinal stability ====================
    // Ordinals are persisted to NBT — reordering modes would corrupt saved data.

    @Test
    @DisplayName("Ordinal values are stable (NBT persistence depends on them)")
    void ordinalValues_areStable() {
        assertThat(SUPPLY.ordinal()).isZero();
        assertThat(RESERVE.ordinal()).isEqualTo(1);
        assertThat(GUARDED.ordinal()).isEqualTo(2);
        assertThat(SEEDED.ordinal()).isEqualTo(3);
        assertThat(SAMPLE.ordinal()).isEqualTo(4);
    }

    @Test
    @DisplayName("valueOf round-trip matches ordinal lookup")
    void valueOf_matchesOrdinalLookup() {
        for (ProviderModule.ProviderMode mode : ProviderModule.ProviderMode.values()) {
            assertThat(ProviderModule.ProviderMode.values()[mode.ordinal()]).isEqualTo(mode);
        }
    }

    // ==================== Available amount calculation ====================
    // These tests document the expected available-amount semantics for each mode.
    // rawAmount=10, isFirstSlotOfType varies.

    @ParameterizedTest(name = "{0}: raw={1} firstSlot={2} => available={3}")
    @DisplayName("Available amount calculation per mode")
    @CsvSource({
        // mode,       raw, firstSlot, expected
        "SUPPLY,        10, true,      10",   // provides everything
        "SUPPLY,        10, false,     10",
        "SUPPLY,         1, true,       1",
        "SUPPLY,         0, true,       0",
        "RESERVE,       10, true,      10",   // RESERVE crops slots, not per-item
        "GUARDED,       10, true,      10",   // GUARDED crops slots, not per-item
        "SEEDED,        10, true,       9",   // leaves 1 per slot
        "SEEDED,         1, true,       0",   // leaves 1 — nothing available
        "SEEDED,         0, true,       0",
        "SAMPLE,        10, true,       9",   // leaves 1 of first occurrence of type
        "SAMPLE,        10, false,     10",   // subsequent slots unaffected
        "SAMPLE,         1, true,       0",   // only 1 present — withheld as sample
    })
    void availableAmount(String modeName, long raw, boolean firstSlot, long expected) {
        ProviderModule.ProviderMode mode = ProviderModule.ProviderMode.valueOf(modeName);
        long result = computeAvailable(mode, raw, firstSlot);
        assertThat(result).isEqualTo(expected);
    }

    /**
     * Mirrors the private ProviderModule.calculateAvailableAmount logic so we can
     * test the algorithm directly without reflection. If that method changes, this
     * mirror must be updated to stay in sync.
     */
    private static long computeAvailable(ProviderModule.ProviderMode mode, long rawAmount, boolean isFirstSlotOfType) {
        if (mode.isHideOnePerSlot()) rawAmount = Math.max(0, rawAmount - 1);
        if (mode.isHideOnePerType() && isFirstSlotOfType) rawAmount = Math.max(0, rawAmount - 1);
        return rawAmount;
    }

    // ==================== setModeFromOrdinal validation ====================

    @Test
    @DisplayName("setModeFromOrdinal rejects negative ordinal")
    void setModeFromOrdinal_rejectsNegative() {
        ProviderModule module = new ProviderModule(64, 4);
        assertThatThrownBy(() -> module.setModeFromOrdinal(null, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    @DisplayName("setModeFromOrdinal rejects ordinal beyond enum length")
    void setModeFromOrdinal_rejectsTooLarge() {
        ProviderModule module = new ProviderModule(64, 4);
        int outOfBounds = ProviderModule.ProviderMode.values().length;
        assertThatThrownBy(() -> module.setModeFromOrdinal(null, outOfBounds))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
