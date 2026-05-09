package com.logistics.core.lib.power;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ServiceLoader;

/**
 * Loader-provided helper: check whether an item is a valid fuel and get its burn duration.
 *
 * <p>Common code calls {@code FuelHelper.INSTANCE} to query fuel information without
 * depending on loader-specific APIs. Each loader provides an implementation via
 * {@code META-INF/services/}:
 * <ul>
 *   <li>Fabric/NeoForge (1.21.11+): delegates to {@code level.fuelValues()}
 *   <li>Fabric (1.21.1): delegates to {@code FuelRegistry} (Fabric API)
 *   <li>NeoForge (1.21.1): delegates to {@code AbstractFurnaceBlockEntity} static methods
 * </ul>
 */
public interface FuelHelper {

    /** Returns {@code true} if {@code stack} can be burned as fuel. */
    boolean isFuel(Level level, ItemStack stack);

    /**
     * Returns the number of ticks {@code stack} burns for, or {@code 0} if it is not a fuel.
     */
    int getBurnDuration(Level level, ItemStack stack);

    FuelHelper INSTANCE = ServiceLoader.load(FuelHelper.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No FuelHelper found"));
}
