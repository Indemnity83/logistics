package com.logistics.pipe.modules;

import com.logistics.pipe.modules.SupplierModule.SupplyMode;
import com.logistics.pipe.network.SupplierModeConfig;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupplierModule.requestAmount")
class SupplierRequestAmountTest {

    private static final int STACK = 64;

    /** A framework lookup that must not be consulted on this branch; fails loudly if it is. */
    private static final LongSupplier NEVER = () -> {
        throw new AssertionError("supplier evaluated on a branch that should not need it");
    };

    private static SupplierModeConfig config(SupplyMode mode) {
        return SupplierModeConfig.forMode(mode, STACK);
    }

    private static long request(SupplyMode mode, long target, long current, long pending,
            LongSupplier space, LongSupplier networkAvailable) {
        return SupplierModule.requestAmount(config(mode), target, current, pending, space, networkAvailable);
    }

    // ==================== INFINITE (window-bounded top-up) ====================

    @Test
    @DisplayName("INFINITE tops up toward free space minus in-flight, capped at the stack window")
    void infinite_topsUpToFreeSpaceMinusPending() {
        assertThat(request(SupplyMode.INFINITE, 0, 0, 10, () -> 50, NEVER)).isEqualTo(40);
    }

    @Test
    @DisplayName("INFINITE is capped at the stack-size window even with lots of free space")
    void infinite_cappedAtWindow() {
        assertThat(request(SupplyMode.INFINITE, 0, 0, 0, () -> 1000, NEVER)).isEqualTo(STACK);
    }

    @Test
    @DisplayName("INFINITE never requests a negative amount when in-flight exceeds free space")
    void infinite_neverNegative() {
        assertThat(request(SupplyMode.INFINITE, 0, 0, 30, () -> 20, NEVER)).isZero();
    }

    // ==================== PARTIAL (restock shortfall, any availability) ====================

    @Test
    @DisplayName("PARTIAL requests the shortfall regardless of network availability")
    void partial_requestsShortfall() {
        assertThat(request(SupplyMode.PARTIAL, 64, 10, 0, NEVER, NEVER)).isEqualTo(54);
    }

    @Test
    @DisplayName("PARTIAL subtracts pending in-flight from the shortfall")
    void partial_accountsForPending() {
        assertThat(request(SupplyMode.PARTIAL, 64, 50, 10, NEVER, NEVER)).isEqualTo(4);
    }

    @Test
    @DisplayName("PARTIAL requests nothing once target is already met by stock plus pending")
    void partial_nothingWhenSatisfied() {
        assertThat(request(SupplyMode.PARTIAL, 64, 60, 10, NEVER, NEVER)).isZero();
    }

    // ==================== FULL (all-or-nothing) ====================

    @Test
    @DisplayName("FULL requests the shortfall only when the network can cover all of it")
    void full_requestsWhenFullyCovered() {
        assertThat(request(SupplyMode.FULL, 64, 0, 0, NEVER, () -> 64)).isEqualTo(64);
    }

    @Test
    @DisplayName("FULL requests nothing when the network cannot cover the whole shortfall")
    void full_nothingWhenPartiallyCovered() {
        assertThat(request(SupplyMode.FULL, 64, 0, 0, NEVER, () -> 30)).isZero();
    }

    @Test
    @DisplayName("FULL accepts an on-demand craftable source (unbounded availability)")
    void full_acceptsCraftable() {
        assertThat(request(SupplyMode.FULL, 64, 0, 0, NEVER, () -> Long.MAX_VALUE)).isEqualTo(64);
    }

    // ==================== Trigger thresholds (BULK modes) ====================

    @Test
    @DisplayName("BULK50 holds off until stock drops to at most half the target")
    void bulk50_waitsForHalf() {
        assertThat(request(SupplyMode.BULK50, 100, 60, 0, NEVER, NEVER)).isZero();
        assertThat(request(SupplyMode.BULK50, 100, 40, 0, NEVER, NEVER)).isEqualTo(60);
    }

    @Test
    @DisplayName("BULK100 restocks only once the inventory is completely empty")
    void bulk100_waitsForEmpty() {
        assertThat(request(SupplyMode.BULK100, 100, 1, 0, NEVER, NEVER)).isZero();
        assertThat(request(SupplyMode.BULK100, 100, 0, 0, NEVER, NEVER)).isEqualTo(100);
    }
}
