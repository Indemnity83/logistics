package com.logistics.core.lib.power;

import java.util.Map;

/**
 * Burn times, in ticks, for the mod's items that work as furnace fuel.
 *
 * <p>The two loaders register fuel by different mechanisms — Fabric through {@code FuelValueEvents},
 * NeoForge through the {@code neoforge:furnace_fuels} data map — so the values live here and each
 * loader reads them rather than restating them. Peat, Bitumen and Tar were missing on NeoForge for
 * several releases because the two lists were maintained independently.
 *
 * <p>Keys are item paths within the mod's namespace. Coal is 1600 for reference.
 */
public final class FurnaceFuels {

    /** Item path to burn time in ticks. */
    public static final Map<String, Integer> BURN_TIMES = Map.of(
            "core/peat", 2000,
            "core/bitumen", 3200,
            "core/tar", 800,
            "core/sawdust", 100);

    private FurnaceFuels() {}
}
