package com.logistics.core.lib.power;

/**
 * Optional capability for machines that can report current network energy demand.
 */
public interface EnergyDemandProvider {
    long networkDemandPerTick();
}
