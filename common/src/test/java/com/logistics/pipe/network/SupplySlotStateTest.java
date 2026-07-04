package com.logistics.pipe.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.pipe.modules.SupplierModule.SupplyMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SupplySlotState")
class SupplySlotStateTest {

    private static SupplierModeConfig partial() {
        return SupplierModeConfig.forMode(SupplyMode.PARTIAL, 64);
    }

    private static SupplySlotState slot(long desired, long onHand, long inbound, long ordered, SupplierModeConfig cfg) {
        return new SupplySlotState("minecraft:cobblestone", desired, onHand, inbound, ordered, cfg);
    }

    @Nested
    @DisplayName("needed")
    class Needed {

        @Test
        @DisplayName("subtracts on-hand, inbound, and ordered from the desired target")
        void subtractsAllDemandSources() {
            assertEquals(65, slot(100, 20, 10, 5, partial()).needed());
        }

        @Test
        @DisplayName("floors at zero when supply already meets the target")
        void floorsAtZeroWhenSatisfied() {
            assertEquals(0, slot(10, 20, 0, 0, partial()).needed());
        }

        @Test
        @DisplayName("floors at zero when inbound plus ordered overshoots the deficit")
        void floorsAtZeroWhenInFlightCovers() {
            assertEquals(0, slot(50, 0, 30, 25, partial()).needed());
        }
    }

    @Nested
    @DisplayName("shouldRestock")
    class ShouldRestock {

        @Test
        @DisplayName("true when there is a deficit and the trigger threshold is met")
        void restocksOnDeficit() {
            assertTrue(slot(64, 5, 0, 0, partial()).shouldRestock());
        }

        @Test
        @DisplayName("false when inbound/ordered already cover the deficit, even with low stock")
        void noRestockWhenInFlightCovers() {
            assertFalse(slot(64, 5, 0, 60, partial()).shouldRestock());
        }

        @Test
        @DisplayName("respects the mode's trigger threshold (BULK50 waits until at/below half)")
        void respectsTriggerThreshold() {
            SupplierModeConfig bulk50 = SupplierModeConfig.forMode(SupplyMode.BULK50, 64);
            // 60/100 on hand is above the 50% trigger, so no restock despite a 40-item deficit
            assertFalse(slot(100, 60, 0, 0, bulk50).shouldRestock());
            // 40/100 is at/below the trigger
            assertTrue(slot(100, 40, 0, 0, bulk50).shouldRestock());
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects a blank item id")
        void rejectsBlankItemId() {
            assertThrows(IllegalArgumentException.class, () -> new SupplySlotState("", 1, 0, 0, 0, partial()));
        }

        @Test
        @DisplayName("rejects negative quantities")
        void rejectsNegatives() {
            assertThrows(IllegalArgumentException.class, () -> slot(-1, 0, 0, 0, partial()));
            assertThrows(IllegalArgumentException.class, () -> slot(0, -1, 0, 0, partial()));
            assertThrows(IllegalArgumentException.class, () -> slot(0, 0, -1, 0, partial()));
            assertThrows(IllegalArgumentException.class, () -> slot(0, 0, 0, -1, partial()));
        }

        @Test
        @DisplayName("rejects a null mode config")
        void rejectsNullConfig() {
            assertThrows(NullPointerException.class, () -> slot(1, 0, 0, 0, null));
        }
    }
}
