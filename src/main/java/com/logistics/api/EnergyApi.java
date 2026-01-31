package com.logistics.api;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

/**
 * API for looking up energy storage on blocks.
 * Uses Fabric's BlockApiLookup for sided block access, similar to ItemStorage.SIDED.
 */
public final class EnergyApi {
    private EnergyApi() {}

    /**
     * Sided lookup for energy storage on blocks.
     * The context parameter is the direction from which the storage is being accessed.
     *
     * <p>For example, if you want to insert energy into the top of a block,
     * use {@code SIDED.find(world, pos, Direction.UP)}.
     */
    public static final BlockApiLookup<EnergyStorage, Direction> SIDED = BlockApiLookup.get(
            Identifier.of("logistics", "energy_storage"),
            EnergyStorage.class,
            Direction.class);
}
