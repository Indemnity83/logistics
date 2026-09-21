package com.logistics.core.lib.power;

import com.logistics.core.lib.energy.IEnergyStorage;

/**
 * A block the logistics network may draw stored RF from.
 *
 * <p>Deliberately separate from the energy capability a block exposes to the world. A machine's
 * buffer is a one-way destination -- energy that reaches it is spent by the machine, not reclaimed
 * over a cable -- so machines expose an insert-only capability to cables and neighbours, and offer
 * extraction only through this interface, which nothing but the logistics network reads.
 *
 * <p>Without the split, the Power Junction's buffer would have to be world-extractable purely so
 * the pipe network could drain it, which also lets a cable network list the junction as a source
 * and pull that RF back out into a battery.
 */
public interface NetworkEnergySupplier {

    /** The buffer the logistics network draws from. Rate-capped by the host's own {@code maxOutput}. */
    IEnergyStorage networkEnergyStorage();
}
