package com.logistics.core.machine.component;

/** The processing role a machine inventory slot plays. */
public enum SlotRole {
    INPUT,
    OUTPUT,
    GENERIC,
    /** Holds {@code UpgradeItem}s read by an {@code UpgradeComponent}; never reachable from a face. */
    UPGRADE
}
