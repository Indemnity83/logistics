package com.logistics.core.lib.power;

/**
 * Marker for blocks that may receive energy <em>only</em> from a directly-adjacent engine — never
 * from cables, batteries, or any loader energy grid (including other mods' conduits).
 *
 * <p>Extraction pipes and the Fluid Pump are powered the classic way: an engine placed against them.
 * They still keep an internal energy buffer (via {@link com.logistics.core.lib.block.capability.HasEnergyStorage}),
 * but that buffer is kept off the loader-exposed energy capability so grid devices cannot find it.
 * Engines deliver by inserting straight into the receiver's {@code energyStorage(side)}.
 *
 * <p>This is the transfer-time complement to {@link AcceptsLowTierEnergy}, which only gates whether a
 * low-tier engine (Redstone Engine) is willing to run.
 */
public interface DirectEnergyReceiver {}
