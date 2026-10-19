package com.logistics.core.machine.control;

/** Synced energy telemetry a GUI reads. Values saturate at {@link Integer#MAX_VALUE}. */
public interface EnergyView {

    /** Whether the host has an RF buffer; when false, {@link #stored} and {@link #capacity} are not shown. */
    boolean hasBuffer();

    /** RF in the buffer. */
    long stored();

    /** RF buffer capacity. */
    long capacity();

    /** RF used (machines) or generated (engines) during the last completed tick. */
    long rate();

    /** Maximum input (machines) or maximum generation (engines) per tick. */
    long maxRate();
}
