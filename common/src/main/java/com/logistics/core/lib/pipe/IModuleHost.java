package com.logistics.core.lib.pipe;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Payload-agnostic abstraction over a block entity that hosts {@link Module}s, exposed to
 * {@link PipeContext}.
 *
 * <p>This is the slice of behavior the cosmetic/connection methods of {@link Module} actually use —
 * module-state storage, energy, connections, redstone, network — with no item-transport coupling. It
 * lets both the item-pipe block entity (via {@link IPipeAccess}) and the fluid-pipe block entity host
 * the same modules without sharing a transport model.
 */
public interface IModuleHost {
    /** Return (or lazily create) the mutable NBT tag for the given module-state key. */
    CompoundTag moduleState(String key);

    /** Return the mutable NBT tag for the given module-state key, if it already exists. */
    @Nullable default CompoundTag existingModuleState(String key) {
        return null;
    }

    /** Remove all stored state for the given module-state key. */
    void clearModuleState(String key);

    /** Return the energy component, or {@code null} if this pipe has no energy storage. */
    @Nullable EnergyComponent getEnergy();

    /** Mark the block entity as changed so it is saved on the next world save. */
    void markDirty();

    /**
     * Return the cached connection type for the given direction.
     * Always uses the value computed on the last tick without re-probing neighbours.
     */
    PipeConnection.Type getCachedConnectionType(Direction direction);

    /**
     * Return the authoritative connection type for the given direction by querying the
     * current world state (may differ from the cached value mid-tick).
     */
    PipeConnection.Type getConnectionType(Level world, BlockPos pos, Direction direction);

    /**
     * Return {@code true} if the block adjacent in {@code direction} is another pipe.
     */
    boolean isNeighborPipe(Level world, BlockPos pos, Direction direction);

    /**
     * Return {@code true} if this pipe is currently receiving redstone power.
     */
    boolean isPowered();

    /**
     * Return the logistics network this pipe belongs to, or {@code null} if not yet formed.
     */
    @Nullable ILogisticsNetwork getNetwork();
}
