package com.logistics.pipe.modules;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.pipe.network.SupplierModeConfig;
import com.logistics.pipe.modules.SupplierModule.SupplyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SupplierModeConfig} — verifies that each {@link SupplyMode}
 * produces the correct trigger threshold and fulfillment semantics.
 *
 * <p>These tests guard against label/ordinal mismatches between the enum and any
 * UI layer that maps ordinals to display names.
 */
class SupplierModeConfigTest {

    private static final int STACK = 64;

    // ===== BULK50 =====

    @Test
    void bulk50_usesPartialFulfillment() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.BULK50, STACK);
        assertEquals(FulfillmentMode.PARTIAL, cfg.fulfillmentMode());
    }

    @Test
    void bulk50_triggersAtOrBelow50Percent() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.BULK50, STACK);
        assertTrue(cfg.isTriggerMet(4, 8),  "50% exactly should trigger");
        assertTrue(cfg.isTriggerMet(3, 8),  "below 50% should trigger");
        assertFalse(cfg.isTriggerMet(5, 8), "above 50% should not trigger");
    }

    @Test
    void bulk50_isNotWindowBounded() {
        assertFalse(SupplierModeConfig.forMode(SupplyMode.BULK50, STACK).isWindowBounded());
    }

    // ===== INFINITE =====

    @Test
    void infinite_usesPartialFulfillment() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.INFINITE, STACK);
        assertEquals(FulfillmentMode.PARTIAL, cfg.fulfillmentMode());
    }

    @Test
    void infinite_isWindowBounded() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.INFINITE, STACK);
        assertTrue(cfg.isWindowBounded(), "INFINITE mode uses a one-stack-at-a-time window");
        assertEquals(STACK, cfg.maxOpenRequestWindow());
    }

    @Test
    void infinite_triggersWhenBelowTarget() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.INFINITE, STACK);
        assertTrue(cfg.isTriggerMet(0, 8));
        assertFalse(cfg.isTriggerMet(8, 8));
    }

    @Test
    void infinite_maxOpenRequestWindow_equalsStackSize() {
        // The window cap is used to compute toRequest = min(maxStack, room - inTransit).
        // It must equal the item's max stack size so we never request more than one stack at once.
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.INFINITE, 16);
        assertEquals(16, cfg.maxOpenRequestWindow());
    }

    // ===== PARTIAL =====

    @Test
    void partial_usesPartialFulfillment() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.PARTIAL, STACK);
        assertEquals(FulfillmentMode.PARTIAL, cfg.fulfillmentMode());
    }

    @Test
    void partial_triggersWhenBelowTarget() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.PARTIAL, STACK);
        assertTrue(cfg.isTriggerMet(7, 8));
        assertFalse(cfg.isTriggerMet(8, 8));
    }

    @Test
    void partial_isNotWindowBounded() {
        assertFalse(SupplierModeConfig.forMode(SupplyMode.PARTIAL, STACK).isWindowBounded());
    }

    // ===== FULL =====

    @Test
    void full_usesFullFulfillment() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.FULL, STACK);
        assertEquals(FulfillmentMode.FULL, cfg.fulfillmentMode(),
                "FULL mode must use FULL fulfillment (all-or-nothing dispatch)");
    }

    @Test
    void full_triggersWhenBelowTarget() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.FULL, STACK);
        assertTrue(cfg.isTriggerMet(7, 8));
        assertFalse(cfg.isTriggerMet(8, 8));
    }

    @Test
    void full_isNotWindowBounded() {
        assertFalse(SupplierModeConfig.forMode(SupplyMode.FULL, STACK).isWindowBounded());
    }

    // ===== BULK100 =====

    @Test
    void bulk100_usesPartialFulfillment() {
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.BULK100, STACK);
        assertEquals(FulfillmentMode.PARTIAL, cfg.fulfillmentMode());
    }

    @Test
    void bulk100_triggersOnlyWhenEmpty() {
        // BULK100 requests in bulk only when completely empty — distinct from PARTIAL
        // which triggers on any deficit. This gives maximum hysteresis: fill all-at-once
        // when the inventory runs dry, then leave it alone until it empties again.
        SupplierModeConfig cfg = SupplierModeConfig.forMode(SupplyMode.BULK100, STACK);
        assertTrue(cfg.isTriggerMet(0, 8),  "triggers when inventory is empty");
        assertFalse(cfg.isTriggerMet(1, 8), "does NOT trigger when any stock is present");
        assertFalse(cfg.isTriggerMet(8, 8), "does NOT trigger when fully stocked");
    }

    @Test
    void bulk100_isNotWindowBounded() {
        assertFalse(SupplierModeConfig.forMode(SupplyMode.BULK100, STACK).isWindowBounded());
    }

    // ===== Ordinal consistency =====
    // Guard against future enum reordering breaking callers that map ordinals to UI labels.

    @Test
    void ordinal_bulk50_is0() {
        assertEquals(0, SupplyMode.BULK50.ordinal());
    }

    @Test
    void ordinal_infinite_is1() {
        assertEquals(1, SupplyMode.INFINITE.ordinal());
    }

    @Test
    void ordinal_partial_is2() {
        assertEquals(2, SupplyMode.PARTIAL.ordinal());
    }

    @Test
    void ordinal_full_is3() {
        assertEquals(3, SupplyMode.FULL.ordinal());
    }

    @Test
    void ordinal_bulk100_is4() {
        assertEquals(4, SupplyMode.BULK100.ordinal());
    }

    @Test
    void totalModeCount_is5() {
        assertEquals(5, SupplyMode.values().length,
                "Adding a mode without updating the GUI switch will break label display");
    }
}
