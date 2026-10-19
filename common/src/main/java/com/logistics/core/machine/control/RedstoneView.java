package com.logistics.core.machine.control;

/** Synced redstone-control state a GUI reads. */
public interface RedstoneView {

    /**
     * Whether the host has a redstone control. On the client, false also means the status has not
     * synced yet; the other values are then meaningless.
     */
    boolean supported();

    /** The current mode. */
    RedstoneMode mode();

    /** Whether the host receives a redstone signal. */
    boolean signal();

    /** Whether the host is permitted to work under its mode and signal. */
    boolean enabled();
}
