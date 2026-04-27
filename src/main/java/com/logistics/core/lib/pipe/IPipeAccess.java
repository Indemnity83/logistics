package com.logistics.core.lib.pipe;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstraction over {@code PipeBlockEntity} exposed to {@link PipeContext}.
 *
 * <p>Keeps {@code core.lib.pipe} free of concrete pipe-domain imports while still
 * giving modules access to the operations they need (module state, energy, connections).
 */
public interface IPipeAccess {
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

    /** Return all items currently traveling through this pipe segment. */
    List<TravelingItem> getTravelingItems();

    /**
     * Inject a new {@link TravelingItem} into this pipe, bypassing capacity checks.
     * Returns {@code true} if the item was accepted.
     */
    boolean forceAddItem(TravelingItem item, Direction fromDirection);
}
