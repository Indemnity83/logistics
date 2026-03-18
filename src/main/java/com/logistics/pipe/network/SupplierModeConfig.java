package com.logistics.pipe.network;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.pipe.modules.SupplierModule.SupplyMode;

/**
 * Normalized configuration derived from a {@link SupplyMode}.
 * Captures the three axes of supplier behaviour as data rather than switch branches:
 * <ul>
 *   <li>{@code triggerThresholdPercent} — what on-hand level triggers a restock request</li>
 *   <li>{@code fulfillmentMode} — whether partial dispatch is acceptable</li>
 *   <li>{@code maxOpenRequestWindow} — max amount to keep in-flight at once</li>
 * </ul>
 */
public record SupplierModeConfig(
        int triggerThresholdPercent,
        FulfillmentMode fulfillmentMode,
        long maxOpenRequestWindow) {

    /**
     * Build a config for the given supply mode.
     *
     * @param mode      the supplier's configured mode
     * @param stackSize item max stack size, used as the window cap for INFINITE mode
     */
    public static SupplierModeConfig forMode(SupplyMode mode, int stackSize) {
        return switch (mode) {
            // triggerThresholdPercent: stock % at or below which a restock is triggered
            //   100 → any deficit (currentAmount < desired)
            //    50 → at or below half (currentAmount ≤ desired/2)
            //     0 → only when completely empty (currentAmount == 0)
            case BULK50 ->   new SupplierModeConfig(50,  FulfillmentMode.PARTIAL, Long.MAX_VALUE);
            case BULK100 ->  new SupplierModeConfig(0,   FulfillmentMode.PARTIAL, Long.MAX_VALUE);
            case PARTIAL ->  new SupplierModeConfig(100, FulfillmentMode.PARTIAL, Long.MAX_VALUE);
            case FULL ->     new SupplierModeConfig(100, FulfillmentMode.FULL,    Long.MAX_VALUE);
            case INFINITE -> new SupplierModeConfig(100, FulfillmentMode.PARTIAL, stackSize);
        };
    }

    /**
     * Return {@code true} when {@code currentAmount} is low enough to trigger a restock.
     * Does not consider pending/in-flight amounts — callers handle that separately.
     *
     * @param currentAmount actual on-hand count in the inventory
     * @param desired       configured target amount
     */
    public boolean isTriggerMet(long currentAmount, long desired) {
        if (triggerThresholdPercent <= 0) return currentAmount == 0;
        if (triggerThresholdPercent >= 100) return currentAmount < desired;
        long threshold = desired * triggerThresholdPercent / 100;
        return currentAmount <= threshold;
    }

    /** {@code true} when this mode uses a bounded in-flight window (INFINITE mode). */
    public boolean isWindowBounded() {
        return maxOpenRequestWindow < Long.MAX_VALUE;
    }
}
