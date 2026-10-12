package com.logistics.core.machine.upgrade;

/**
 * An item that modifies a machine from its upgrade slot. Implemented by the {@code Item} class; a
 * machine's {@link UpgradeComponent} sums the contributions of every installed copy.
 *
 * <p>Contributions default to "no effect", so an upgrade overrides only what it changes.
 */
public interface UpgradeItem {

    /** Additive range bonus one installed copy adds to {@code op}. */
    default int rangeBonus(OperationKey op) {
        return 0;
    }

    /** Most copies of this upgrade a single upgrade slot holds. */
    int maxPerSlot();
}
