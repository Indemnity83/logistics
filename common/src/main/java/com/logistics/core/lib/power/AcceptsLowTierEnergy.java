package com.logistics.core.lib.power;

import net.minecraft.core.Direction;

/**
 * Interface for blocks that can receive energy from {@link LowTierEnergySource} implementations.
 *
 * <p>Blocks must explicitly implement this interface to receive energy from early-game engines
 * like the Redstone Engine.
 *
 * <p>The default implementation accepts low-tier energy from all directions. Override
 * {@link #acceptsLowTierEnergyFrom} to implement sided restrictions if needed.
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * public class EarlyGameMachineBlockEntity extends BlockEntity implements AcceptsLowTierEnergy {
 *     // Uses default implementation - accepts from all directions
 * }
 *
 * public class SidedMachineBlockEntity extends BlockEntity implements AcceptsLowTierEnergy {
 *     @Override
 *     public boolean acceptsLowTierEnergyFrom(Direction from) {
 *         return from == Direction.DOWN; // Only accepts from bottom
 *     }
 * }
 * }</pre>
 */
public interface AcceptsLowTierEnergy {
    /**
     * Determines whether this block accepts low-tier energy from the specified direction.
     *
     * @param from the direction from which energy is being received
     * @return true if this block accepts low-tier energy from the given direction
     */
    default boolean acceptsLowTierEnergyFrom(Direction from) {
        return true;
    }
}
