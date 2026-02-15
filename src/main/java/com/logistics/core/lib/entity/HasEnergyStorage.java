package com.logistics.core.lib.entity;

import com.logistics.core.lib.energy.EnergyComponent;
import team.reborn.energy.api.EnergyStorage;

/**
 * Marker interface for block entities that expose energy storage via Team Reborn Energy API.
 * <p>
 * Use {@link EnergyComponent} to implement this easily.
 */
public interface HasEnergyStorage {
    EnergyStorage energyStorage();
}