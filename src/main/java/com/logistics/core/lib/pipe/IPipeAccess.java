package com.logistics.core.lib.pipe;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction over {@code PipeBlockEntity} exposed to {@link PipeContext}.
 *
 * <p>Keeps {@code core.lib.pipe} free of concrete pipe-domain imports while still
 * giving modules access to the operations they need (module state, energy, connections).
 */
public interface IPipeAccess {
    /** Return (or lazily create) the mutable NBT tag for the given module-state key. */
    CompoundTag moduleState(String key);

    /** Remove all stored state for the given module-state key. */
    void clearModuleState(String key);

    /** Return the energy component, or {@code null} if this pipe has no energy storage. */
    @Nullable EnergyComponent getEnergy();

    /** Mark the block entity as changed so it is saved on the next world save. */
    void setChanged();

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
}
